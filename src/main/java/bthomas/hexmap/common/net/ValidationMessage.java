package bthomas.hexmap.common.net;

import bthomas.hexmap.Main;
import bthomas.hexmap.client.Client;
import bthomas.hexmap.common.json.JsonConversionException;
import bthomas.hexmap.common.util.JsonUtils;
import bthomas.hexmap.logging.HexmapLogger;
import bthomas.hexmap.server.ConnectionHandler;
import bthomas.hexmap.server.Server;
import com.google.gson.JsonObject;

import java.util.HashSet;


public class ValidationMessage extends HexMessage
{

    private static final long serialVersionUID = 3239519580113436622L;

    public String username;
    private final static String usernameKey = "name";
    public String password;
    private final static String passwordKey = "password";

    public ValidationMessage(String username, String password)
    {
        this.username = username;
        this.password = password;
    }

    public ValidationMessage(String username)
    {
        this(username, null);
    }

    /**
     Dummy constructor to use with JSON serialization
     */
    public ValidationMessage()
    {
        this(null, null);
    }

    /**
     Constructs an object from JSON representation

     @param root
     The JsonObject containing the data for this object.

     @throws JsonConversionException
     If there is not proper data stored in the JsonObject
     */
    public ValidationMessage(JsonObject root) throws JsonConversionException
    {
        username = JsonUtils.getString(root, usernameKey);
        password = JsonUtils.getString(root, passwordKey);
    }

    @Override
    public ValidationMessage fromJson(JsonObject root) throws JsonConversionException
    {
        return new ValidationMessage(root);
    }

    @Override
    public void applyToServer(Server server, ConnectionHandler source)
    {
        username = username.trim();

        //reject blank usernames
        if(username.length() == 0)
        {
            String reason = "invalid username";
            Main.logger.log(HexmapLogger.INFO, "Rejected new connection for: " + reason);
            server.closeListener(source, reason);
        }

        //reject duplicate logins
        if(server.hasConnectedUser(username))
        {
            String reason = "requested duplicate username: " + username;
            Main.logger.log(HexmapLogger.INFO, "Rejected new connection for: " + reason);
            server.closeListener(source, reason);
        }

        //validate username/password
        if(!server.hasRegisteredUser(username))
        {
            if(password != null)
            {
                server.registerNewUser(username, password);
            }
            server.initConnection(source, username);
        }
        else if(server.validateUser(username, password))
        {
            server.initConnection(source, username);
        }
        else
        {
            String reason = "invalid password";
            Main.logger.log(HexmapLogger.INFO, "Rejected new connection for: " + username + " " + reason);
            server.closeListener(source, reason);
        }
    }

    @Override
    public void buildJson(JsonObject root, HashSet<Object> loopDetector) throws JsonConversionException
    {
        root.addProperty(usernameKey, username);
        root.addProperty(passwordKey, password);
        super.buildJson(root, loopDetector);
    }

    @Override
    public void applyToClient(Client client)
    {
        //
    }

    @Override
    public long getKey()
    {
        return serialVersionUID;
    }
}
