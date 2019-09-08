package bthomas.hexmap.server;

import bthomas.hexmap.Main;
import bthomas.hexmap.common.net.PingMessage;

public class ServerKeepaliveManager implements Runnable
{
    private final Server server;

    public static final int PING_INTERVAL_MILLIS = 15_000;

    public ServerKeepaliveManager(Server server)
    {
        this.server = server;
    }

    @Override
    public void run()
    {
        for(ConnectionHandler client: server.getClients())
        {
            if(client.lastPingReceived < client.lastPingSent)
            {
                server.closeListener(client, "Connection timed out.");
            }
            else
            {
                client.lastPingSent = System.currentTimeMillis();
                client.addMessage(new PingMessage(false));
            }
        }
        Main.scheduleTask(this, System.currentTimeMillis() + PING_INTERVAL_MILLIS);
    }
}
