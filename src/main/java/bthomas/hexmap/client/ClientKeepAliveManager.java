package bthomas.hexmap.client;

import bthomas.hexmap.Main;
import bthomas.hexmap.common.net.PingMessage;

public class ClientKeepAliveManager implements Runnable
{
    private final Client client;

    public static final int PING_INTERVAL_MILlIS = 15_000;

    public ClientKeepAliveManager(Client client)
    {
        this.client = client;
    }

    @Override
    public void run()
    {
        if(client.connected)
        {
            if(client.lastPingReceived < client.lastPingSent)
            {
                client.disconnect("Connection times out");
            }
            else
            {
                client.lastPingSent = System.currentTimeMillis();
                client.sendMessage(new PingMessage(true));
            }
        }
        Main.scheduleTask(this, System.currentTimeMillis() + PING_INTERVAL_MILlIS);
    }
}
