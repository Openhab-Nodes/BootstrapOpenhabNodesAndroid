package org.libBSTbootstrap.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.MenuItem;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.openhab.bootstrap.R;
import org.libBSTbootstrap.adapter.BootstrapDeviceAdapter;
import org.libBSTbootstrap.adapter.onSelectionChange;
import org.libBSTbootstrap.deviceList.BootstrapDeviceUpdateListener;
import org.libBSTbootstrap.deviceList.BootstrapDevicesViaWifiScan;
import org.libBSTbootstrap.BootstrapService;
import org.libBSTbootstrap.WirelessNetwork;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

public class DetectAndBindActivity extends AppCompatActivity implements ServiceConnection, Toolbar.OnMenuItemClickListener, View.OnClickListener, BootstrapDeviceUpdateListener, onSelectionChange, BootstrapService.BootstrapServicePermissions {
    @Bind(R.id.toolbar) Toolbar toolbar;
    @Bind(R.id.progressBar) ProgressBar progressBar;
    @Bind(R.id.list) RecyclerView list;
    @Bind(R.id.fab) FloatingActionButton btnOK;
    @Bind(R.id.emptyText) TextView emptyText;
    @Bind(R.id.addText) TextView pwdText;

    BootstrapDeviceAdapter mDevicesAdapter;
    BootstrapDevicesViaWifiScan bootstrapDevicesDiscovery = new BootstrapDevicesViaWifiScan();

    BootstrapService mService;

    // Called when the connection with the service is established
    public void onServiceConnected(ComponentName className, IBinder service) {
        // Because we have bound to an explicit
        // service that is running in our own process, we can
        // cast its IBinder to a concrete class and directly access it.
        BootstrapService.LocalBinder binder = (BootstrapService.LocalBinder) service;
        mService = binder.getService();
        mService.checkPermissionsAndStart(this, list, this);
        mDevicesAdapter.setData(mService.getBootstrapDeviceList());
    }

    // Called when the connection with the service disconnects unexpectedly
    public void onServiceDisconnected(ComponentName className) {
        mService = null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_with_list);
        ButterKnife.bind(this);

        btnOK.setOnClickListener(this);

        mDevicesAdapter = new BootstrapDeviceAdapter(this);
        mDevicesAdapter.setOnSelectionChangeListener(this);
        mDevicesAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                emptyText.setVisibility(mDevicesAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
                checkConditions();
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
        mDevicesAdapter.notifyDataSetChanged();

        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(mDevicesAdapter);
        list.setItemAnimator(new DefaultItemAnimator());
        list.setHasFixedSize(true);

        bootstrapDevicesDiscovery.addObserver(this);
        bootstrapDevicesDiscovery.addObserver(mDevicesAdapter);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == BootstrapService.REQUEST_LOCATION)
            mService.checkPermissionsAndStart(this, list, this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, BootstrapService.class);
        bindService(intent, this, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        bootstrapDevicesDiscovery.stop(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mService != null)
            mService.checkPermissionsAndStart(this, list, this);

        bootstrapDevicesDiscovery.start(this);
    }

    void checkConditions() {
        if (mDevicesAdapter == null || mService == null)
            return;

        boolean ok = mDevicesAdapter.isOneSelected() && mService.isStarted();
        btnOK.setVisibility(ok ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        int id = item.getItemId();
        View view = findViewById(android.R.id.content);
        if (view == null)
            return false;

        return false;
    }

    @Override
    public void onClick(View view) {
        if (mService == null)
            return;

        mService.addBSData(getIntent().getExtras());
        Intent intent = new Intent(this, BootstrapActivity.class);
        intent.putExtra("mode", mService.getBootstrapDeviceList().containsUnbound() ? BootstrapService.Mode.BindMode.name() : BootstrapService.Mode.DeviceInfoMode.name());
        startActivity(intent);
    }

    @Override
    public void updateFinished(List<WirelessNetwork> entries) {
        progressBar.setVisibility(View.INVISIBLE);
    }

    @Override
    public void updateStarted() {
        progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void onSelectionChanged() {
        checkConditions();
    }


    @Override
    public void servicePermissionsGranted() {
        bootstrapDevicesDiscovery.start(this);
        checkConditions();
    }
}
