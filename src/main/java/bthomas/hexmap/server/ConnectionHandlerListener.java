package bthomas.hexmap.server;

import bthomas.hexmap.common.json.JsonConversionException;
import bthomas.hexmap.common.net.HexMessage;
import bthomas.hexmap.common.util.JsonUtils;
import bthomas.hexmap.logging.HexmapLogger;
import bthomas.hexmap.Main;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import java.io.BufferedReader;
import java.io.IOException;

/**
 This class handles incoming data from a client connection

 @author Brendan Thomas
 @since 2017-10-28 */
public class ConnectionHandlerListener implements Runnable
{

    private ConnectionHandler parent;
    private BufferedReader input;

    public boolean stopped = false;

    /**
     @param parent
     The main handler for this connection
     @param input
     The input stream to read from
     */
    public ConnectionHandlerListener(ConnectionHandler parent, BufferedReader input)
    {
        this.parent = parent;
        this.input = input;
    }

    public void run()
    {
        while(true)
        {
            try
            {
                String message = input.readLine();
                if(message != null)
                {
                    JsonElement inputMessage = Main.PARSER.parse(message);
                    if(!(inputMessage instanceof JsonObject))
                    {
                        throw new JsonConversionException("Received message not a HexMessage object: " + message);
                    }

                    JsonObject messageJson = (JsonObject) inputMessage;
                    long key = JsonUtils.getLong(messageJson, "key");
                    HexMessage template = parent.parent.getRegisteredMessage(key);
                    if(template == null)
                    {
                        throw new JsonConversionException("Unregistered message key: " + key);
                    }
                    HexMessage messageObj = template.fromJson(messageJson);

                    //send message to server with source
                    parent.parent.receiveMessage(new MessageData(messageObj, parent));
                }
                else
                {
                    throw new IOException("Null received from socket input stream");
                }
            }
            catch(JsonConversionException | JsonSyntaxException e)
            {
                Main.logger.log(HexmapLogger.SEVERE,
                        "Error occurred while parsing input message: " + HexmapLogger.getStackTraceString(e));
                parent.parent.closeListener(parent, "Invalid JSON received from input stream");
                break;
            }
            catch(IOException e)
            {
                //not actually an error if socket was supposed to be closed
                if(!stopped)
                {
                    Main.logger.log(HexmapLogger.SEVERE,
                            "Error reading from input stream: " + HexmapLogger.getStackTraceString(e));
                    parent.parent.closeListener(parent, "Error reading from input stream");
                }
                //something happened to the stream, close the connection
                break;
            }
        }
    }
}
