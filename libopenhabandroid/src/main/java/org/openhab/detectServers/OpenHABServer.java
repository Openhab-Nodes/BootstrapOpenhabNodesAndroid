package org.openhab.detectServers;

import android.content.Context;

import org.openhab.R;

import de.duenndns.ssl.MemorizingTrustManager;

/**
 * Represents a connection to an OpenHab Server. Autodetect will use mDNS Service Discovery and
 * fill in host + port. You may add a server manually by entering the URI + username + password for it.
 */
public class OpenHABServer {
    public final boolean discovered;
    public String password = null;
    public String username = null;
    private String url;
    private String host;
    private int port;
    private CheckConnectivity checkConnectivity = new CheckConnectivity(this);
    private int itemCount;
    private int sitemapCount;
    private int thingsCount;
    private Connectivity connectivity = Connectivity.NotReachable;
    private boolean secure;
    public OpenHABServer(String host, int port, boolean secure) {
        setHost(host, port, secure);
        discovered = true;
    }

    public OpenHABServer() {
        discovered = false;
    }

    public boolean setDetails(int itemCount, int sitemapCount, int thingsCount) {
        boolean changed = this.itemCount != itemCount || this.sitemapCount != sitemapCount || this.thingsCount != thingsCount;
        this.itemCount = itemCount;
        this.sitemapCount = sitemapCount;
        this.thingsCount = thingsCount;
        return changed;
    }

    public void updateConnectivity(int index, CheckConnectivity.OpenHABConnectivityListener listener) {
        checkConnectivity.check(index, listener);
    }

    public void setMemorizingTrustManager(MemorizingTrustManager mtm) {
        checkConnectivity.setMemorizingTrustManager(mtm);
    }

    public void setHost(String host, int port, boolean secure) {
        this.host = host;
        this.port = port;
        this.secure = secure;
        this.url = (secure?"https://":"http://") + host+":"+String.valueOf(port);
        this.connectivity = Connectivity.ReachableAccessDenied;
    }

    public boolean isValid() {
        return url != null && host != null;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
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
        this.secure = url.startsWith("https:");
        this.connectivity = Connectivity.ReachableAccessDenied;
    }

    public String getHost() {
        return host;
    }

    /**
     * @return Return a stringified version of all necessary information for an openHab server
     * (url + username + password). The information pieces are separated by tabulators.
     */
    public String getBootstrapString() {
        return url + "\t" + (username!=null?username:"") + "\t" + (password!=null?password:"");
    }

    public String getDetailsString(Context context) {
        return context.getString(R.string.openhab_server_details,
                (password != null ? password : context.getString(R.string.openhab_no_password)),
                secure ? "Secure (Https)" : "Plain (Http)",
                sitemapCount, thingsCount, itemCount
        );
    }

    public void setCredentials(String username, String password) {
        this.username = username;
        if (this.username.length() == 0)
            this.username = null;
        this.password = password;
        if (this.password.length() == 0)
            this.password = null;
    }

    public Connectivity getConnectivity() {
        return connectivity;
    }

    public boolean setConnectivity(Connectivity connectivity) {
        boolean changed = this.connectivity != connectivity;
        this.connectivity = connectivity;
        return changed;
    }

    public int getPort() {
        return port;
    }

    public boolean isSecure() {
        return secure;
    }

    public enum Connectivity {
        NotReachable,
        ReachableAccessDenied,
        ConnectionError, Reachable
    }
}
