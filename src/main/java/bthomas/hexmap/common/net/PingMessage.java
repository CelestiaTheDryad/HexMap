package bthomas.hexmap.common.net;

import bthomas.hexmap.client.Client;
import bthomas.hexmap.common.json.JsonConversionException;
import bthomas.hexmap.common.util.JsonUtils;
import bthomas.hexmap.server.ConnectionHandler;
import bthomas.hexmap.server.Server;
import com.google.gson.JsonObject;

import java.util.HashSet;

public class PingMessage extends HexMessage
{
    private static final long serialVersionUID = -4105997739745042111L;

    private final boolean fromClient;
    private static final String fromClientKey = "fromClient";

    /**
     Dummy constructor to use with JSON serialization
     */
    public PingMessage()
    {
        fromClient = false;
    }

    /**
     Main use constructor

     @param fromClient If this ping originated from a client, false indicates ping originated from server
     */
    public PingMessage(boolean fromClient)
    {
        this.fromClient = fromClient;
    }

    /**
     Constructs an object from JSON representation

     @param root
     The JsonObject containing the data for this object.

     @throws JsonConversionException
     If there is not proper data stored in the JsonObject
     */
    public PingMessage(JsonObject root) throws JsonConversionException
    {
        fromClient = JsonUtils.getBoolean(root, fromClientKey);
    }

    @Override
    public void applyToServer(Server server, ConnectionHandler source)
    {
        if(fromClient)
        {
            source.addMessage(this);
        }
        else
        {
            source.lastPingReceived = System.currentTimeMillis();
        }
    }

    @Override
    public void applyToClient(Client client)
    {
        if(fromClient)
        {
            client.lastPingReceived = System.currentTimeMillis();
        }
        else
        {
            client.sendMessage(this);
        }
    }

    @Override
    public long getKey()
    {
        return serialVersionUID;
    }

    @Override
    public void buildJson(JsonObject root, HashSet<Object> loopDetector) throws JsonConversionException
    {
        super.buildJson(root, loopDetector);
        root.addProperty(fromClientKey, fromClient);
    }

    @Override
    public HexMessage fromJson(JsonObject root) throws JsonConversionException
    {
        return new PingMessage(root);
    }
}
