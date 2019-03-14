package bthomas.hexmap.server;

import bthomas.hexmap.Logging.HexmapLogger;
import bthomas.hexmap.Main;
import bthomas.hexmap.commands.*;
import bthomas.hexmap.common.Unit;
import bthomas.hexmap.net.HexMessage;
import bthomas.hexmap.net.InitMessage;
import bthomas.hexmap.net.MoveUnitMessage;
import bthomas.hexmap.net.NewUnitMessage;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class contains the main server code for Hexmap. It can handle connections
 * from a theoretically infinite number of clients. It also has support for server
 * console commands.
 */
public class Server {

    public ServerSocket serverService = null;
    private boolean closing = false;
    private HashMap<String, HexCommand> commands = new HashMap<>();
    private HashMap<String, ConnectionHandler> usernameMap = new HashMap<>();
    private Random rand = new Random();

    //thread handling
    private final ReentrantLock threadHandlerLock = new ReentrantLock();
    private ArrayList<ConnectionHandler> listenerThreads = new ArrayList<>();

    //message receiving
    private ArrayDeque<MessageData> arrivalQueue = new ArrayDeque<>();
    private final ReentrantLock arrivalQueueLock = new ReentrantLock();

    //battlefield info
    private final ReentrantLock boardLock = new ReentrantLock();
    private int x = 10;
    private int y = 10;
    private HashMap<Integer, Unit> units = new HashMap<>();


    /**
     * Standard constructor, creates a server on port 7777
     */
    public Server() {
        try {
            //TODO: allow custom ports
            serverService = new ServerSocket(7777);
        }
        catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        new Thread(this::handleCommands).start();
        new Thread(this::handleMessages).start();

        Main.logger.log(HexmapLogger.INFO, "Server init.");
        registerAllCommands();
        beginListening();
    }


    /**
     * Listen for and handle new connections
     * Runs until the server closes
     */
    private void beginListening() {
        try {
            //set timeout on ServerSocket.accept()
            serverService.setSoTimeout(250);
        }
        catch (SocketException e) {
            System.err.println("ServerSocket error");
            return;
        }

        while (!closing) {
            try {
                //Main.logger.log("Accept");
                Socket service = serverService.accept();

                threadHandlerLock.lock();

                //race condition protection
                if(closing) {
                    threadHandlerLock.unlock();
                    break;
                }

                //setup handler for new connection
                ConnectionHandler runner = new ConnectionHandler(this, service);
                new Thread(runner).start();

                listenerThreads.add(runner);
                Main.logger.log(HexmapLogger.INFO, "Made new listener. Total :" + listenerThreads.size());
                threadHandlerLock.unlock();
            }
            catch (SocketTimeoutException e) {
                //we timeout this loop every 1/4 second to check for server closes
                //perhaps there's a more elegant way to do this?
            }
            catch (IOException e) {
                if(!closing) {
                    Main.logger.log(HexmapLogger.ERROR, "error accepting connection");
                }
            }
        }
    }


    /**
     * Registers all "vanilla" commands for Hexmap
     */
    private void registerAllCommands() {
        //TODO: there's probably a better way to do this
        registerCommand(new RollCommand());
        registerCommand(new StopCommand());
        registerCommand(new SetupCommand());
        registerCommand(new AddUnitCommand());
    }


    /**
     * Registers a given command object
     *
     * @param command The command to register
     * @return True if the command was registered successfully, false if otherwise
     */
    public boolean registerCommand(HexCommand command) {
        //reject default keys
        if(command.getKey().equals("")) {
            return false;
        }

        //register this command if it's a new key
        if(commands.get(command.getKey()) == null) {
            commands.put(command.getKey(), command);
            return true;
        }

        //reject duplicate keys
        else {
            return false;
        }
    }


    /**
     * Handles commands typed into the server console
     * Runs infinitely until the server closes
     *
     * The blocking on keyboard.nextline() might cause some problems,
     * but none have been noticed so far
     */
    private void handleCommands() {
        Scanner keyboard = new Scanner(System.in);
        while(!closing) {
            String command = keyboard.nextLine();

            //do nothing on empty messages
            if(command.equals("")) {
                continue;
            }

            if(command.charAt(0) == '/') {
                command = command.substring(1);
            }
            String[] parts = command.split(" ", 2);

            HexCommand serverCommand = commands.get(parts[0]);
            if(serverCommand != null) {
                //use null to represent a command with no extra parts
                serverCommand.applyFromServer(this, parts.length == 2 ? parts[1] : null);
            }
            else {
                Main.logger.log(HexmapLogger.INFO, "Unknown command: " + parts[0]);
            }
        }
    }


    /**
     * Handles messages received from clients
     * Runs infinitely until the server closes
     */
    public void handleMessages() {
        while(!closing) {
            arrivalQueueLock.lock();
            MessageData message = arrivalQueue.poll();
            arrivalQueueLock.unlock();

            if(message != null) {
                message.message.ApplyToServer(this, message.source);
            }
            //if no message in queue, wait a bit
            else {
                try {
                    Thread.sleep(50);
                }
                catch (InterruptedException e) {
                    //no problem
                }
            }
        }

    }


    /**
     * Checks to see if the server has a connected user with a given name
     *
     * @param username The name to check for
     * @return True if there is a connected player with that name, false if not.
     */
    public boolean hasConnectedUser(String username) {
        return usernameMap.containsKey(username);
    }


    /**
     * Adds a given unit to the board
     * Assumes the unit is valid
     *
     * @param u The unit to add
     */
    public void addUnit(Unit u) {
        boardLock.lock();
        units.put(u.UID, u);
        boardLock.unlock();
    }


    /**
     * Moves a unit based on a message from a client
     * Assumes the move is valid
     * In case of clients trying to simultaneously move units, operates on first-come, first-serve
     *
     * @param message The move message to apply
     */
    public void moveUnit(MoveUnitMessage message) {
        boardLock.lock();
        Unit u = units.get(message.unitUID);
        if(u != null) {
            Main.logger.log(HexmapLogger.INFO, String.format("Unit: %s moved from %d, %d to %d, %d", u.name, u.locX,
                    u.locY, message.toX, message.toY));
            u.locX = message.toX;
            u.locY = message.toY;
            sendAll(message);
        }
        boardLock.unlock();
    }


    /**
     * Sends setup instructions to a newly connected client
     *
     * @param source The connection handler to set up
     * @param username The username of the new client
     */
    public void initConnection(ConnectionHandler source, String username) {
        Main.logger.log(HexmapLogger.INFO, "Accepted new connection with name: " + username);
        source.username = username;
        usernameMap.put(username, source);
        //give client the map info
        boardLock.lock();
        source.addMessage(new InitMessage(x, y, -1));
        for(Unit u: units.values()) {
            source.addMessage(new NewUnitMessage(u));
        }
        boardLock.unlock();
    }


    /**
     * Sends a message to all connected clients
     *
     * @param message The message to send
     */
    public void sendAll(HexMessage message) {
        //requiring messages to bounce from client to server back to client
        //guarantees message order is the same between all clients
        threadHandlerLock.lock();
        Object[] threads = listenerThreads.toArray();
        for(int i = 0; i < threads.length; i++) {
            ConnectionHandler thread = (ConnectionHandler) threads[i];
            thread.addMessage(message);
        }
        threadHandlerLock.unlock();
    }


    /**
     * Callback method for the server to receive messages
     * Adds to a queue so the server can process messages single-threaded
     *
     * @param message The message to process
     */
    public void receiveMessage(MessageData message) {
        arrivalQueueLock.lock();
        arrivalQueue.add(message);
        arrivalQueueLock.unlock();
    }


    /**
     * Closes a client connection
     *
     * @param listener The connection to close
     */
    public void closeListener(ConnectionHandler listener) {
        //prevent multiple closes
        if(listener.isClosed || listener.toClose) {
            return;
        }

        if(listener.username != null) {
            usernameMap.remove(listener.username);
        }
        //nicely close handler if it's running
        if(!listener.isClosed) {
            listener.toClose = true;


            while(!listener.isClosed) {
                //wait a bit
                try {
                    Thread.sleep(50);
                }
                catch (InterruptedException e) {
                    //TODO: figure out what to do with this
                    //Not sure what the various causes are, can be called manually
                    //can also be caused from outside (from OS?) in which case I think program should shut down immediately
                }
            }
        }
        threadHandlerLock.lock();
        listenerThreads.remove(listener);
        if(listener.username != null) {
            Main.logger.log(HexmapLogger.INFO, "Disconnected client: " + listener.username);
        }
        threadHandlerLock.unlock();
    }


    /**
     * Nicely close the server and exit with a given status
     *
     * @param retStat The status to exit with
     */
    public void closeServer(int retStat) {
        closing = true;

        Main.logger.log(HexmapLogger.INFO, "closing server, goodbye");

        //safely close all listeners
        threadHandlerLock.lock();
        while(listenerThreads.size() > 0) {
            closeListener(listenerThreads.get(0));
        }
        threadHandlerLock.unlock();

        try {
            serverService.close();
        }
        catch (IOException e) {
            e.printStackTrace();
            System.exit(99);
        }

        System.exit(retStat);

    }

   /* ========================================================================================

   Getters and setters

   ========================================================================================= */

    public HexCommand getCommand(String key) {
        return commands.get(key);
    }

    public Random getRandom() {
        return rand;
    }

    public void setSize(int x, int y) {
        //TODO: make this change work with connected clients
        boardLock.lock();
        this.x = x;
        this.y = y;
        boardLock.unlock();
    }
}
