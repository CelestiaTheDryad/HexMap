package bthomas.hexmap.common.net;

import bthomas.hexmap.common.json.JsonConversionException;
import bthomas.hexmap.common.util.JsonUtils;
import bthomas.hexmap.logging.HexmapLogger;
import bthomas.hexmap.Main;
import bthomas.hexmap.client.Client;
import bthomas.hexmap.server.ConnectionHandler;
import bthomas.hexmap.server.Server;
import com.google.gson.JsonObject;

import javax.swing.*;
import java.util.HashSet;

/**
 This message contains a chat message from a player

 @author Brendan Thomas
 @since 2019-02-15 */
public class ChatMessage extends HexMessage
{
    private static final long serialVersionUID = 147350107808482294L;

    public final String text;
    public final static String textKey = "text";

    /**
     Standard constructor

     @param text
     The message as to be printed in the chat box
     */
    public ChatMessage(String text)
    {
        this.text = text;
    }

    /**
     Dummy constructor to use with JSON serialization
     */
    public ChatMessage()
    {
        this((String) null);
    }

    /**
     Constructs an object from JSON representation

     @param root
     The JsonObject containing the data for this object.

     @throws JsonConversionException
     If there is not proper data stored in the JsonObject
     */
    public ChatMessage(JsonObject root) throws JsonConversionException
    {
        text = JsonUtils.getString(root, textKey);
    }

    @Override
    public ChatMessage fromJson(JsonObject root) throws JsonConversionException
    {
        return new ChatMessage(root);
    }

    @Override
    public void buildJson(JsonObject root, HashSet<Object> loopDetector) throws JsonConversionException
    {
        super.buildJson(root, loopDetector);
        root.addProperty(textKey, text);
    }

    @Override
    public void applyToClient(Client client)
    {
        client.waitForGUI();
        Main.logger.log(HexmapLogger.INFO, text);
        SwingUtilities.invokeLater(() -> client.chatAppend(text));
    }

    @Override
    public void applyToServer(Server server, ConnectionHandler source)
    {
        String newText = source.username + ": " + text;
        Main.logger.log(HexmapLogger.INFO, newText);
        server.sendAll(new ChatMessage(newText));
    }

    @Override
    public long getKey()
    {
        return serialVersionUID;
    }
}
