package org.openhab.detectServers;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.nsd.NsdManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityOptionsCompat;
import android.view.View;
import android.widget.ProgressBar;

import org.openhab.detectServers.adapter.Adapter;

import de.duenndns.ssl.MemorizingTrustManager;

/**
 * Provides all means to implement an activity that lists all available OpenHab servers
 * from the network neighbourhood (via mDNS-SD).
 */
public class OpenHabActivityUtils implements DiscoverListener {
    ProgressBar progress;
    View emptyView;

    Adapter mServerAdapter;
    Discover mDiscover;

    public Adapter getServerAdapter() {
        return mServerAdapter;
    }

    public void createView(Activity activity, View emptyView, ProgressBar progress) {
        this.emptyView = emptyView;
        this.progress = progress;

        if (mServerAdapter == null)
            mServerAdapter = new Adapter(activity);

        if (mDiscover == null) {
            mDiscover = new Discover((NsdManager) activity.getSystemService(Context.NSD_SERVICE));
        } else {
            mDiscover.clearObservers();
        }

        mDiscover.addObserver(this);
        mDiscover.addObserver(mServerAdapter);
        mServerAdapter.notifyDataSetChanged();
    }


    /**
     * Returns the selected OpenHAB server if it is reachable, otherwise
     * opens a dialog to make the user enter credentials and return null.
     *
     * @param activity To show a dialog, we need the parent activity.
     * @return Returns an openhab Server or null.
     */
    public OpenHABServer checkServerAndProceed(Activity activity, View view) {
        OpenHABServer server = mServerAdapter.getSelected();
        if (server.getConnectivity() != OpenHABServer.Connectivity.Reachable) {
            showOpenHabServerEditDialog(activity, view, server);
            return null;
        }
        return server;
    }

    public void showOpenHabServerEditDialog(@NonNull Activity activity, @NonNull View view, @NonNull OpenHABServer server) {
        Intent intent = new Intent(activity, EditServerActivity.class);
        EditServerActivity.server = server;
        ActivityOptionsCompat options = ActivityOptionsCompat.
                makeSceneTransitionAnimation(activity, view, EditServerActivity.EXTRA_TRANSITION_IMAGE);
        activity.startActivityForResult(intent, EditServerActivity.REQUEST_CODE, options.toBundle());
    }

    @Override
    public void onOpenHABDiscovered(OpenHABServer server) {
    }

    @Override
    public void onOpenHABDiscoveryStarted() {
        emptyView.setVisibility(View.GONE);
        progress.setVisibility(View.VISIBLE);
    }

    @Override
    public void onOpenHABDiscoveryFinished(FinishedState state) {
        progress.setVisibility(View.INVISIBLE);
        emptyView.setVisibility(mServerAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    public void stopDiscovery(Context context) {
        mDiscover.stopDiscovery(context);
    }

    public void startDiscovery(Context context, MemorizingTrustManager memorizingTrustManager) {
        mDiscover.startDiscovery(context, memorizingTrustManager);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == EditServerActivity.REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            mServerAdapter.onOpenHABDiscovered(EditServerActivity.server);
        }
        EditServerActivity.server = null;
    }
}
