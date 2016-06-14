package org.libbootstrapiotdevice.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import org.libbootstrapiotdevice.BootstrapService;
import org.libbootstrapiotdevice.adapter.BootstrapDeviceAdapter;
import org.libbootstrapiotdevice.network.BootstrapCore;

public class BootstrapActivityUtils implements ServiceConnection {
    private BootstrapServiceReady bootstrapServiceReady;
    private BootstrapService mService;
    private BootstrapDeviceAdapter mDevicesAdapter;

    public void onCreate(Context context, RecyclerView list, final TextView emptyView, BootstrapServiceReady bootstrapServiceReady) {
        this.bootstrapServiceReady = bootstrapServiceReady;

        mDevicesAdapter = new BootstrapDeviceAdapter(context);
        mDevicesAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                emptyView.setVisibility(mDevicesAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                onChanged();
            }

            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                onChanged();
            }
        });

        list.setLayoutManager(new LinearLayoutManager(context));
        list.setAdapter(mDevicesAdapter);
        list.setItemAnimator(new DefaultItemAnimator());
        list.setHasFixedSize(true);

        mDevicesAdapter.setSelectionEnabled(true);
    }

    // Called when the connection with the service is established
    @Override
    public void onServiceConnected(ComponentName className, IBinder service) {
        BootstrapService.LocalBinder binder = (BootstrapService.LocalBinder) service;
        mService = binder.getService();
        BootstrapCore bootstrapCore = mService.getBootstrapCore();
        bootstrapCore.addChangeListener(mDevicesAdapter);
        mDevicesAdapter.setData(bootstrapCore.getDevices());
        if (bootstrapServiceReady != null)
            bootstrapServiceReady.onBootstrapServiceReady();
    }

    @Override
    public void onServiceDisconnected(ComponentName className) {
        mService = null;
    }

    public BootstrapDeviceAdapter getDevicesAdapter() {
        return mDevicesAdapter;
    }

    public BootstrapService getService() {
        return mService;
    }

    public interface BootstrapServiceReady {
        void onBootstrapServiceReady();
    }
}
