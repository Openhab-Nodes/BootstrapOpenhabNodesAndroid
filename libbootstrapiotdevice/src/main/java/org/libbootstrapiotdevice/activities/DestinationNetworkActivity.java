package org.libbootstrapiotdevice.activities;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.Snackbar;
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
        implements ServiceConnection, onSelectionChange, View.OnClickListener, BootstrapService.Callback {
    Button btnOK;
    Button btnRetry;
    Toolbar toolbar;
    ProgressBar progress;
    RecyclerView list;
    TextView emptyView;

    // Dialog
    Button btnDialogOk, btnDialogTest;
    View dialogView;

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

        for (BootstrapDevice device : mService.getBootstrapDevices().getDevices()) {
            mAdapter.addDevice(device);
        }
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

        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        dialogView = getLayoutInflater().inflate(R.layout.dialog_test_wifi_credentials, null, false);
        builder.setView(dialogView)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                })
                .setNeutralButton(R.string.test_wifi_credentials, null);
        // Create the AlertDialog object and return it
        final AlertDialog dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            ProgressBar progressBar;
            @Override
            public void onShow(DialogInterface dialogInterface) {
                btnDialogOk = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                btnDialogTest = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
                progressBar = ((ProgressBar)dialogView.findViewById(R.id.progress));

                btnDialogOk.setEnabled(false);
                btnDialogOk.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        BootstrapData.instance().setWifiData(mAdapter.getSelectedNetwork());
                        Intent intent = new Intent(DestinationNetworkActivity.this, BootstrapActivity.class);
                        startActivity(intent);
                        dialog.dismiss();
                    }
                });
                btnDialogOk.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(final View view) {
                        String password = ((TextView)dialogView.findViewById(R.id.password)).getText().toString();

                        btnDialogTest.setEnabled(false);
                        WirelessNetwork network = mAdapter.getSelectedNetwork();
                        network.pwd = password;
                        mService.testWifi(DestinationNetworkActivity.this, network.ssid, network.pwd);
                    }
                });
            }
        });
        dialog.show();
    }

    @Override
    public void wifiSuccess(boolean success) {
        btnDialogTest.setEnabled(true);
        if (success) {
            btnDialogOk.setEnabled(true);
        } else {
            Snackbar.make(list, "Credentials wrong!", Snackbar.LENGTH_SHORT).show();
        }
    }
}
