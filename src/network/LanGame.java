package network;

import java.net.InetAddress;

// Una partida abierta en la red, tal como la vio LanBrowser: quién la creó y dónde conectarse.
public final class LanGame {
    private final String id;
    private final String hostName;
    private final InetAddress address;
    private final int port;

    public LanGame(String id, String hostName, InetAddress address, int port) {
        this.id = id;
        this.hostName = hostName;
        this.address = address;
        this.port = port;
    }

    public String getId() { return id; }
    public String getHostName() { return hostName; }
    public InetAddress getAddress() { return address; }
    public int getPort() { return port; }
}
