package org.libbootstrapiotdevice.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.libbootstrapiotdevice.BootstrapData;
import org.libbootstrapiotdevice.BootstrapDevice;
import org.libbootstrapiotdevice.BootstrapService;
import org.libbootstrapiotdevice.R;
import org.libbootstrapiotdevice.WirelessNetwork;
import org.libbootstrapiotdevice.adapter.OverlappingNetworksAdapter;
import org.libbootstrapiotdevice.adapter.onSelectionChange;

public class DestinationNetworkActivity extends AppCompatActivity
        implements ServiceConnection, onSelectionChange, View.OnClickListener {
    Button btnOK;
    Button btnRetry;
    Toolbar toolbar;
    ProgressBar progress;
    RecyclerView list;
    TextView emptyView;

    BootstrapService mService;
    private OverlappingNetworksAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_with_list);
        btnOK = (Button)findViewById(R.id.btnNext);
        btnRetry = (Button)findViewById(R.id.btnRetry);
        toolbar = (Toolbar)findViewById(R.id.toolbar);
        list = (RecyclerView)findViewById(R.id.list);
        emptyView = (TextView)findViewById(R.id.emptyText);
        progress = (ProgressBar)findViewById(R.id.progress);
        setSupportActionBar(toolbar);
        btnOK.setOnClickListener(this);
        btnOK.setEnabled(false);
        btnRetry.setVisibility(View.GONE);
        emptyView.setText(R.string.no_networks_in_range);

        mAdapter = new OverlappingNetworksAdapter(this);
        mAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                emptyView.setVisibility(mAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
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
        mAdapter.notifyDataSetChanged();


        mAdapter.setOnSelectionChangeListener(this);
        onSelectionChanged();

        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(mAdapter);
        list.setItemAnimator(new DefaultItemAnimator());
        list.setHasFixedSize(true);
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

        for (BootstrapDevice device : mService.getBootstrapCore().getDevices()) {
            mAdapter.addDevice(device);
        }

        mAdapter.setSelectedNetwork(mService.getLastNetworkSSID());
    }

    // Called when the connection with the service disconnects unexpectedly
    public void onServiceDisconnected(ComponentName className) {
        mService = null;
    }

    @Override
    public void onSelectionChanged() {
        btnOK.setEnabled(mAdapter.getSelectedNetwork() != null);
    }

    @SuppressLint("InflateParams")
    @Override
    public void onClick(View view) {
        if (mService == null)
            return;

        WirelessNetwork network = mAdapter.getSelectedNetwork();
        if (network == null)
            return;

        Intent intent = new Intent(this, TestNetworkActivity.class);
        intent.putExtra("wifiname", network.ssid);
        startActivityForResult(intent, TestNetworkActivity.REQUEST_CODE);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == TestNetworkActivity.REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            String pwd = data.getExtras().getString("wifipwd");
            WirelessNetwork network = mAdapter.getSelectedNetwork();
            network.pwd = pwd;

            BootstrapData.instance().setWifiData(network);
            Intent intent = new Intent(DestinationNetworkActivity.this, BootstrapActivity.class);
            startActivity(intent);
        }
    }
}
