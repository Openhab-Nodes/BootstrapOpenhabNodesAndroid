package org.openhab.detectServers;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;

/**
 * Created by david on 26.04.16.
 */
public class OpenHABReachability {
    public static final String TAG = "OpenHABReachability";

    public interface OpenHABReachabilityListener {
        void openhabReachable(@NonNull OpenHABServer server, int index);
    }
    public OpenHABReachability(@NonNull final OpenHABServer server, final int index, @NonNull final OpenHABReachabilityListener listener) {
        Log.w(TAG, "OpenHABReachability "+server.url+" index: "+String.valueOf(index));
        new AsyncTask<OpenHABServer, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(OpenHABServer... servers) {
                try {
                    servers[0].reachable = false;
                    Log.w(TAG, "Connect to "+servers[0].host + String.valueOf(servers[0].port));
                    Socket s = new Socket();
                    s.connect(new InetSocketAddress(servers[0].host, servers[0].port), 1000);
                    Log.d(TAG, "Socket connected");
                    s.close();
                    servers[0].reachable = true;
                    return true;
                } catch (MalformedURLException e) {
                    Log.e(TAG, e.getMessage());
                    return false;
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage());
                    return false;
                }
            }


            @Override
            protected void onPostExecute(Boolean aBoolean) {
                listener.openhabReachable(server,index);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, server);
    }
}
