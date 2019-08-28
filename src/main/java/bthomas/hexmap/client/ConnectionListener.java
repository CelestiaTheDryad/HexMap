package bthomas.hexmap.client;

import bthomas.hexmap.common.json.JsonConversionException;
import bthomas.hexmap.common.util.JsonUtils;
import bthomas.hexmap.logging.HexmapLogger;
import bthomas.hexmap.Main;
import bthomas.hexmap.common.net.HexMessage;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * This class handles incoming data from the connected server
 *
 * @author Brendan Thomas
 * @since 2017-10-21
 */
public class ConnectionListener implements Runnable{

    private Client parent;
    private BufferedReader input;

    /**
     * The standard constructor
     *
     * @param parent The Hexmap client this listener is used for
     * @param input The data input stream to listen to
     */
    public ConnectionListener(Client parent, BufferedReader input) {
        this.parent = parent;
        this.input = input;
    }

    @Override
    public void run() {
        while (true) {
            try {
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
                    HexMessage template = parent.getRegisteredMessage(key);
                    if(template == null)
                    {
                        throw new JsonConversionException("Unregistered message key: " + key);
                    }
                    HexMessage messageObj = template.fromJson(messageJson);

                    messageObj.applyToClient(parent);
                }
                else
                {
                    throw new IOException("Null received from socket input stream");
                }
            }
            catch(JsonConversionException | JsonSyntaxException e)
            {
                Main.logger.log(HexmapLogger.ERROR,
                        "Error occurred while parsing input message: " + HexmapLogger.getStackTraceString(e));
                parent.disconnect("Invalid JSON received from input stream");
                break;
            }
            catch (IOException e) {
                //only a problem if parent is supposed to be connected
                if(parent.connected) {
                    Main.logger.log(HexmapLogger.SEVERE, "Error reading from input stream: " + HexmapLogger.getStackTraceString(e));
                    parent.disconnect("Connection error.");
                }
                //something happened to the stream, close the connection
                break;
            }
        }
    }
}
