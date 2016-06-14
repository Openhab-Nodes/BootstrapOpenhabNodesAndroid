package org.openhab.detectServers;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;

import de.duenndns.ssl.MemorizingTrustManager;

/**
 * Check the connectivity of a given openhab server.
 */
public class CheckConnectivity {
    public static final String TAG = "CheckConnectivity";
    final private OpenHABServer server;
    SSLContext sslContext = null;
    private boolean isChecking = false;
    private de.duenndns.ssl.MemorizingTrustManager mtm;

    public CheckConnectivity(OpenHABServer server) {
        this.server = server;
    }

    public void setMemorizingTrustManager(MemorizingTrustManager mtm) {
        this.mtm = mtm;
        HttpsURLConnection.setDefaultHostnameVerifier(
                mtm.wrapHostnameVerifier(HttpsURLConnection.getDefaultHostnameVerifier()));
        try {
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new X509TrustManager[]{mtm}, new java.security.SecureRandom());
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
            this.sslContext = null;
            this.mtm = null;
        }
    }

    @Nullable
    private OpenHabRestItemResult[] getItems(URL url) throws IOException, NoSuchAlgorithmException, KeyManagementException {
        URLConnection urlConnection = url.openConnection();
        urlConnection.setDoInput(true);
        urlConnection.setDoOutput(false);
        urlConnection.setRequestProperty("Accept", "application/json");
        if (mtm != null && urlConnection instanceof HttpsURLConnection) {
            HttpsURLConnection connection = (HttpsURLConnection) urlConnection;
            connection.setSSLSocketFactory(sslContext.getSocketFactory());
        }
        try {
            urlConnection.connect();
        } catch (IOException e) {
            throw new IOException(e);
        }

        HttpURLConnection httpURLConnection = (HttpURLConnection) urlConnection;
        if (httpURLConnection.getResponseCode() != 200) {
            return null;
        }

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));

        String data = "";
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            data += line + "\n";
        }
        bufferedReader.close();
        Gson gson = new Gson();
        OpenHabRestItemResult[] items = new OpenHabRestItemResult[0];

        try {
            items = gson.fromJson(data, OpenHabRestItemResult[].class);
        } catch (JsonSyntaxException ignored) {
            Log.e(TAG, "parse error: " + data);
            return items;
        }
        return items;
    }

    public void check(final int index, @NonNull final OpenHABConnectivityListener listener) {
        if (isChecking)
            return;

        isChecking = true;

        Log.w(TAG, "CheckConnectivity " + server.getUrl() + " index: " + String.valueOf(index));
        new AsyncTask<OpenHABServer, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(OpenHABServer... servers) {
                boolean changed = servers[0].setConnectivity(OpenHABServer.Connectivity.NotReachable);
                try {
                    OpenHabRestItemResult[] items = getItems(new URL(servers[0].getUrl() + "/rest/items"));
                    if (items != null) {
                        int itemCount = items.length;
                        int sitemapCount = 0;
                        int thingsCount = 0;
                        changed |= servers[0].setConnectivity(OpenHABServer.Connectivity.Reachable);

                        items = getItems(new URL(servers[0].getUrl() + "/rest/sitemaps"));
                        if (items != null) {
                            sitemapCount = items.length;

                            items = getItems(new URL(servers[0].getUrl() + "/rest/things"));
                            if (items != null) {
                                thingsCount = items.length;
                            }
                        }
                        changed |= servers[0].setDetails(itemCount, sitemapCount, thingsCount);
                    } else
                        changed |= servers[0].setConnectivity(OpenHABServer.Connectivity.ReachableAccessDenied);

                } catch (NoSuchAlgorithmException | IOException | KeyManagementException e) {
                    Log.e(TAG, e.getMessage());
                    changed |= servers[0].setConnectivity(OpenHABServer.Connectivity.ConnectionError);
                }
                return changed;
            }

            @Override
            protected void onPostExecute(Boolean changed) {
                listener.openhabConnectivityChanged(server, index, changed);
                isChecking = false;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, server);
    }

    public interface OpenHABConnectivityListener {
        void openhabConnectivityChanged(@NonNull OpenHABServer server, int index, boolean changed);
    }

    class OpenHabRestItemResult {
        String name = "";

        OpenHabRestItemResult() {
            // no-args constructor
        }
    }
}
