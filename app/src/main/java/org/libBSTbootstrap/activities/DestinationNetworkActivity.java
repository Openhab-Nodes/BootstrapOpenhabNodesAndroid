package org.libBSTbootstrap.activities;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.openhab.bootstrap.R;
import org.libBSTbootstrap.adapter.OverlappingNetworksAdapter;
import org.libBSTbootstrap.adapter.onSelectionChange;
import org.libBSTbootstrap.BootstrapDevice;
import org.libBSTbootstrap.BootstrapService;
import org.libBSTbootstrap.WirelessNetwork;
import org.libBSTbootstrap.network.WifiUtils;

import java.net.InetAddress;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

public class DestinationNetworkActivity extends AppCompatActivity implements ServiceConnection, onSelectionChange, View.OnClickListener, WifiUtils.Callback {
    @Bind(R.id.toolbar) Toolbar toolbar;
    @Bind(R.id.progressBar) ProgressBar progressBar;
    @Bind(R.id.list) RecyclerView list;
    @Bind(R.id.fab) FloatingActionButton btnOK;
    @Bind(R.id.emptyText) TextView emptyText;
    @Bind(R.id.addText) TextView pwdText;

    BootstrapService mService;
    private OverlappingNetworksAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_with_list);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);
        btnOK.setOnClickListener(this);
        btnOK.setVisibility(View.GONE);
        emptyText.setText(R.string.no_networks_in_range);
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

        initListView();
    }

    private void initListView() {
        if (mService.getBootstrapDeviceList().devices.size()==0)
            throw new RuntimeException("No devices to configure!");

        mAdapter = new OverlappingNetworksAdapter(this);
        mAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                emptyText.setVisibility(mAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
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

        for (BootstrapDevice device : mService.getBootstrapDeviceList().devices) {
            mAdapter.addDevice(device);
        }

        mAdapter.setOnSelectionChangeListener(this);
        onSelectionChanged();

        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(mAdapter);
        list.setItemAnimator(new DefaultItemAnimator());
        list.setHasFixedSize(true);
    }

    // Called when the connection with the service disconnects unexpectedly
    public void onServiceDisconnected(ComponentName className) {
        mService = null;
    }

    @Override
    public void onSelectionChanged() {
        btnOK.setVisibility(mAdapter.getSelectedNetwork() == null ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onClick(View view) {
        if (mService == null)
            return;

        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Check wireless network").setMessage("Your selected wireless network and credentials are tested now")
            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    WirelessNetwork network = mAdapter.getSelectedNetwork();
                    network.pwd = pwdText.getText().toString();
                    mService.checkWifi(network, DestinationNetworkActivity.this);
                }
            }).setNegativeButton(android.R.string.cancel, null);
        builder.create().show();
    }

    @Override
    public void connected(boolean connected, List<InetAddress> addresses) {

    }

    @Override
    public void canConnect(boolean canConnect) {
        if (canConnect) {
            mService.updateBSWifi(mAdapter.getSelectedNetwork());
            Intent intent = new Intent(this, BootstrapActivity.class);
            startActivity(intent);
        } else {
            Snackbar.make(list, "Credentials wrong!", Snackbar.LENGTH_SHORT).show();
        }
    }
}
