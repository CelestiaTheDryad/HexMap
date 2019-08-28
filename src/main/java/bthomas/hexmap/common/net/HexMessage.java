package bthomas.hexmap.common.net;

import bthomas.hexmap.client.Client;
import bthomas.hexmap.common.json.JsonConversionException;
import bthomas.hexmap.common.json.JsonSerializable;
import bthomas.hexmap.server.ConnectionHandler;
import bthomas.hexmap.server.Server;
import com.google.gson.JsonObject;

import java.io.Serializable;
import java.util.HashSet;

/***
 * Base class for HexMap networking.
 * Inherit from this class to add new communication types.
 *
 * @author Brendan Thomas
 * @since 2019-02-15
 */
public abstract class HexMessage implements Serializable, JsonSerializable
{
    private static final long serialVersionUID = 7376065318780735728L;

    @Override
    public void buildJson(JsonObject root, HashSet<Object> loopDetector) throws JsonConversionException
    {
        root.addProperty("key", getKey());
    }

    @Override
    public HexMessage fromJson(JsonObject root) throws JsonConversionException
    {
        throw new UnsupportedOperationException("Base HexMessage fromJson: " + this.getClass());
    }

    /**
     * Apply this message to a server
     *
     * @param server The server to apply to
     * @param source The connection that this message came from
     */
    public abstract void applyToServer(Server server, ConnectionHandler source);

    /**
     * Apply this message to a client
     *
     * @param client The client to apply to
     */
    public abstract void applyToClient(Client client);

    /**
     * Gets a unique identifier for this hexmessage
     *
     * @return the Id
     */
    public abstract long getKey();
}
