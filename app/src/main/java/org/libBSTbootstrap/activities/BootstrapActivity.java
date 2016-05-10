package org.libBSTbootstrap.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.openhab.bootstrap.R;
import org.libBSTbootstrap.adapter.BootstrapDeviceAdapter;
import org.libBSTbootstrap.BootstrapDevice;
import org.libBSTbootstrap.deviceList.BootstrapDeviceUpdateListener;
import org.libBSTbootstrap.deviceList.BootstrapDevicesViaWifiScan;
import org.libBSTbootstrap.BootstrapService;
import org.libBSTbootstrap.WirelessNetwork;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.Bind;
import butterknife.ButterKnife;

public class BootstrapActivity extends AppCompatActivity implements ServiceConnection, View.OnClickListener, BootstrapService.BootstrapProgressListener, BootstrapDeviceUpdateListener {
    @Bind(R.id.toolbar) Toolbar toolbar;
    @Bind(R.id.progressBar) ProgressBar progressBar;
    @Bind(R.id.list) RecyclerView list;
    @Bind(R.id.fab) FloatingActionButton btnOK;
    @Bind(R.id.emptyText) TextView emptyText;
    @Bind(R.id.addText) TextView pwdText;

    BootstrapService mService;
    BootstrapService.Mode mode;
    private BootstrapDeviceAdapter mDevicesAdapter;
    private BootstrapDevicesViaWifiScan bootstrapDevicesDiscovery = new BootstrapDevicesViaWifiScan();
    private boolean allFailed;


    Timer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_with_list);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);
        if (getSupportActionBar()==null)
            throw new RuntimeException();

        toolbar.setTitle("Configure nodes");

        Intent intent = getIntent();
        if (intent == null)
            throw new RuntimeException("Intent expected");

        mode = BootstrapService.Mode.valueOf(intent.getStringExtra("mode"));
        if (mode == null)
            throw new RuntimeException("Mode parameter expected");

        switch (mode) {
            case UnknownMode:
                break;
            case BindMode:
                getSupportActionBar().setSubtitle(R.string.bind_devices_to_app);
                break;
            case DeviceInfoMode:
                getSupportActionBar().setSubtitle(R.string.request_wifi_networks);
                break;
            case BootstrapMode:
                getSupportActionBar().setSubtitle(R.string.bootstrap_devices);
                break;
        }

        btnOK.setOnClickListener(this);
        btnOK.setVisibility(View.GONE);

        initNodesView();
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


    // Called when the connection with the service is established
    public void onServiceConnected(ComponentName className, IBinder service) {
        // Because we have bound to an explicit
        // service that is running in our own process, we can
        // cast its IBinder to a concrete class and directly access it.
        BootstrapService.LocalBinder binder = (BootstrapService.LocalBinder) service;
        mService = binder.getService();
        mService.setProgressListener(this);
        mDevicesAdapter.setData(mService.getBootstrapDeviceList());
        if (mService.getBootstrapDeviceList().devices.size()>0)
            start();
    }

    private void start() {
        btnOK.setVisibility(View.INVISIBLE);
        progressBar.setVisibility(View.VISIBLE);
        if (mode == BootstrapService.Mode.DeviceInfoMode) {
            mService.getBootstrapDeviceList().clear();
            mDevicesAdapter.notifyDataSetChanged();
            bootstrapDevicesDiscovery.start(this);
        } else
            mService.startProcess(mode);
    }

    // Called when the connection with the service disconnects unexpectedly
    public void onServiceDisconnected(ComponentName className) {
        mService = null;
    }

    private void initNodesView() {
        mDevicesAdapter = new BootstrapDeviceAdapter(this);
        mDevicesAdapter.setSelectionEnabled(false);
        mDevicesAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {

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

        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(mDevicesAdapter);
        list.setItemAnimator(new DefaultItemAnimator());
        list.setHasFixedSize(true);

        bootstrapDevicesDiscovery.addObserver(this);
        bootstrapDevicesDiscovery.addObserver(mDevicesAdapter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        bootstrapDevicesDiscovery.stop(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mService != null && mService.getBootstrapDeviceList().devices.size()>0)
            start();
    }


    @Override
    public void onClick(View view) {
        switch (mode) {
            case UnknownMode:
                break;
            case BindMode:
                start();
                break;
            case DeviceInfoMode:
                if (allFailed)
                    start();
                else
                    startActivity(new Intent(this, DestinationNetworkActivity.class));
                break;
            case BootstrapMode:
                startActivity(new Intent(this, DetectAndBindActivity.class));
                break;
        }
    }

    @Override
    public void BootstrapProgress(BootstrapDevice device, int current, int total, int waitTimeMS) {
        mDevicesAdapter.notifyItemChanged(current);

        if (timer != null) {
            timer.cancel();
            timer = null;
        }

        if (waitTimeMS == -1) {
            progressBar.setIndeterminate(true);
        } else {
            progressBar.setIndeterminate(false);
            progressBar.setMax(waitTimeMS);
            progressBar.setProgress(0);
            timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    if (progressBar.getProgress()+30>=progressBar.getMax()) {
                        progressBar.setProgress(progressBar.getMax());
                        if (timer != null)
                            timer.cancel();
                        timer = null;
                        return;
                    }
                    progressBar.setProgress(progressBar.getProgress()+30);
                }
            }, 30, 30);
        }
    }

    @Override
    public void BootstrapFinished() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        progressBar.setVisibility(View.INVISIBLE);
        btnOK.setVisibility(View.VISIBLE);
        mService.restoreWifi();

        switch (mode) {
            case UnknownMode:
                break;
            case BindMode:
                btnOK.setImageResource(android.R.drawable.ic_media_previous);

                allFailed = true;
                for (BootstrapDevice device: mService.getBootstrapDeviceList().devices) {
                    if (device.is_bound) {
                        allFailed = false;
                        break;
                    }
                }

                btnOK.setImageResource(allFailed ? android.R.drawable.ic_menu_revert : android.R.drawable.ic_media_play);

                if (!allFailed)
                    mode = BootstrapService.Mode.DeviceInfoMode;
                break;
            case DeviceInfoMode:
                allFailed = true;
                for (BootstrapDevice device: mService.getBootstrapDeviceList().devices) {
                    if (device.reachableNetworks != null && device.reachableNetworks.size()>0) {
                        allFailed = false;
                        break;
                    }
                }

                btnOK.setImageResource(allFailed ? android.R.drawable.ic_menu_revert : android.R.drawable.ic_media_play);
                break;
            case BootstrapMode:
                btnOK.setImageResource(android.R.drawable.ic_media_previous);
                break;
        }
    }

    @Override
    public void updateFinished(List<WirelessNetwork> entries) {
        bootstrapDevicesDiscovery.stop(this);
        BootstrapFinished();
        mService.startProcess(BootstrapService.Mode.DeviceInfoMode);
    }

    @Override
    public void updateStarted() {
        BootstrapProgress(null,0,1,2000);
    }
}
