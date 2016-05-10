package org.openhab.detectServers;

/**
 * Represents a connection to an OpenHab Server. Autodetect will use mDNS Service Discovery and
 * fill in host + port. You may add a server manually by entering the URI + username + password for it.
 */
public class OpenHABServer {
    public String url;
    public String host;
    public String password = null;
    public String username = null;
    int port;
    public boolean reachable = false;
    public boolean discovered = true;

    public OpenHABServer(String host, int port, boolean secure) {
        this.host = host;
        this.port = port;
        this.url = (secure?"https://":"http://") + host+":"+String.valueOf(port);
    }

    public OpenHABServer(String url) {
        try {
            int pos = url.lastIndexOf(':');
            if (pos == -1)
                return;
            port = Integer.valueOf(url.substring(pos + 1));
            host = url.substring(0, pos);
            pos = url.indexOf("://");
            if (pos == -1)
                return;
            host = host.substring(pos + 3);
        } catch (Exception ignored) {
            return;
        }
        this.url = url;
    }

    public String getBootstrapString() {
        return url + "\t" + (username!=null?username:"") + "\t" + (password!=null?password:"");
    }
}
