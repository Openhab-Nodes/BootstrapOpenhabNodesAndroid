package org.openhab_node.bootstrapopenhabnodes;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.nsd.NsdManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.openhab_node.bootstrapopenhabnodes.adapter.BootstrapDeviceAdapter;
import org.openhab_node.bootstrapopenhabnodes.adapter.onSelectionChange;
import org.openhab_node.bootstrapopenhabnodes.bootstrap.BootstrapDeviceUpdateListener;
import org.openhab_node.bootstrapopenhabnodes.bootstrap.BootstrapDevicesViaWifiScan;
import org.openhab_node.bootstrapopenhabnodes.bootstrap.BootstrapService;
import org.openhab_node.bootstrapopenhabnodes.adapter.OpenHABAdapter;
import org.openhab_node.bootstrapopenhabnodes.bootstrap.WirelessNetwork;
import org.openhab_node.bootstrapopenhabnodes.bootstrap.network.WifiUtils;
import org.openhab_node.bootstrapopenhabnodes.openhab.OpenHABDiscover;
import org.openhab_node.bootstrapopenhabnodes.openhab.OpenHABDiscoverListener;
import org.openhab_node.bootstrapopenhabnodes.openhab.OpenHABReachability;
import org.openhab_node.bootstrapopenhabnodes.openhab.OpenHABServer;

import java.net.InetAddress;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

public class DetectActivity extends AppCompatActivity implements ServiceConnection, Toolbar.OnMenuItemClickListener, View.OnClickListener, BootstrapDeviceUpdateListener, OpenHABDiscoverListener, onSelectionChange, BootstrapService.BootstrapServicePermissions {
    @Bind(R.id.fab) FloatingActionButton fab;

    @Bind(R.id.toolbarOpenhabDiscovery) Toolbar toolbarOpenhabDiscovery;
    @Bind(R.id.servers) RecyclerView serverView;
    @Bind(R.id.serversEmpty) View serversEmpty;
    @Bind(R.id.progressServer) ProgressBar progressServer;

    //@Bind(R.id.toolbarNodesDiscovery) Toolbar toolbarNodesDiscovery;
    @Bind(R.id.nodes) RecyclerView nodesView;
    @Bind(R.id.nodesEmpty) View nodesEmpty;
    @Bind(R.id.progressNodes) ProgressBar progressNodes;

    BootstrapDeviceAdapter mDevicesAdapter;
    BootstrapDevicesViaWifiScan bootstrapDevicesDiscovery = new BootstrapDevicesViaWifiScan();

    OpenHABAdapter mServerAdapter;
    OpenHABDiscover mOpenHABDiscover;

    BootstrapService mService;

    // Called when the connection with the service is established
    public void onServiceConnected(ComponentName className, IBinder service) {
        // Because we have bound to an explicit
        // service that is running in our own process, we can
        // cast its IBinder to a concrete class and directly access it.
        BootstrapService.LocalBinder binder = (BootstrapService.LocalBinder) service;
        mService = binder.getService();
        mService.checkPermissionsAndStart(this, serverView, this);
        mDevicesAdapter.setData(mService.getBootstrapDeviceList());
    }

    // Called when the connection with the service disconnects unexpectedly
    public void onServiceDisconnected(ComponentName className) {
        mService = null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detect);
        ButterKnife.bind(this);

        toolbarOpenhabDiscovery.inflateMenu(R.menu.menu_detect);
        toolbarOpenhabDiscovery.setOnMenuItemClickListener(this);

        fab.setOnClickListener(this);

        initNodesView();
        initOpenhabView();
    }

    private void initNodesView() {
        mDevicesAdapter = new BootstrapDeviceAdapter(this);
        mDevicesAdapter.setOnSelectionChangeListener(this);
        mDevicesAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                nodesEmpty.setVisibility(mDevicesAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
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

        nodesView.setLayoutManager(new LinearLayoutManager(this));
        nodesView.setAdapter(mDevicesAdapter);
        nodesView.setItemAnimator(new DefaultItemAnimator());
        nodesView.setHasFixedSize(true);

        bootstrapDevicesDiscovery.addObserver(this);
        bootstrapDevicesDiscovery.addObserver(mDevicesAdapter);
    }

    private void initOpenhabView() {
        mServerAdapter = new OpenHABAdapter(this);
        mServerAdapter.setOnSelectionChangeListener(this);
        mServerAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                serversEmpty.setVisibility(mServerAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
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

        serverView.setLayoutManager(new LinearLayoutManager(this));
        serverView.setAdapter(mServerAdapter);
        serverView.setItemAnimator(new DefaultItemAnimator());
        serverView.setHasFixedSize(true);

        mOpenHABDiscover = new OpenHABDiscover((NsdManager) getSystemService(Context.NSD_SERVICE));
        mOpenHABDiscover.addObserver(this);
        mOpenHABDiscover.addObserver(mServerAdapter);

        mServerAdapter.notifyDataSetChanged();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        mService.checkPermissionsAndStart(this, serverView, this);
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
        mOpenHABDiscover.stopDiscovery(this);
        bootstrapDevicesDiscovery.stop(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mOpenHABDiscover.startDiscovery(this);

        if (mService != null)
            mService.checkPermissionsAndStart(this, serverView, this);

        bootstrapDevicesDiscovery.start(this);
    }

    void checkConditions() {
        if (mServerAdapter == null || mDevicesAdapter == null || mService == null)
            return;

        boolean ok = mServerAdapter.getSelected() != null && mDevicesAdapter.isOneSelected() && mService.isStarted();
        fab.setVisibility(ok ? View.VISIBLE : View.INVISIBLE);
    }

    void showOpenhabServerEditDialog() {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        @SuppressLint("InflateParams")
        final View dialogView = getLayoutInflater().inflate(R.layout.openhab_server, null);
        builder.setView(dialogView)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                })
                .setNeutralButton(R.string.openhab_test_server, null);
        // Create the AlertDialog object and return it
        final AlertDialog dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            Button btnOk, btnTest;
            ProgressBar progressBar;
            OpenHABServer server;
            @Override
            public void onShow(DialogInterface dialogInterface) {
                btnOk = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                btnTest = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
                progressBar = ((ProgressBar)dialogView.findViewById(R.id.progress));

                btnOk.setEnabled(false);
                btnOk.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mServerAdapter.onOpenHABDiscovered(server);
                        dialog.dismiss();
                    }
                });
                btnTest.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(final View view) {
                        String url = ((TextView)dialogView.findViewById(R.id.url)).getText().toString();

                        server = new OpenHABServer(url);
                        if (server.url == null) {
                            Snackbar.make(view, "Server address invalid", Snackbar.LENGTH_SHORT).show();
                            return;
                        }
                        server.discovered = false;
                        server.username = ((TextView)dialogView.findViewById(R.id.username)).getText().toString();
                        if (server.username.length() == 0)
                            server.username = null;
                        server.password = ((TextView)dialogView.findViewById(R.id.password)).getText().toString();
                        if (server.password.length() == 0)
                            server.password = null;
                        progressBar.setVisibility(View.VISIBLE);
                        new OpenHABReachability(server, 0, new OpenHABReachability.OpenHABReachabilityListener() {
                            @Override
                            public void openhabReachable(@NonNull OpenHABServer server, int index) {
                                progressBar.setVisibility(View.INVISIBLE);
                                btnOk.setEnabled(server.reachable);
                                if (server.reachable)
                                    Snackbar.make(view, "Server reached", Snackbar.LENGTH_SHORT).show();
                                else
                                    Snackbar.make(view, "Server not reachable", Snackbar.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
            }
        });
        dialog.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        int id = item.getItemId();
        View view = findViewById(android.R.id.content);
        if (view == null)
            return false;

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_add_openhab_server) {
            showOpenhabServerEditDialog();
            return true;
        }
        return false;
    }

    @Override
    public void onClick(View view) {
        if (mService == null)
            return;

        mService.updateBSDataAdditional(mServerAdapter.getSelected().getBootstrapString());
        Intent intent = new Intent(this, ConfigureNodesActivity.class);
        intent.putExtra("mode", mService.getBootstrapDeviceList().containsUnbound() ? BootstrapService.Mode.BindMode.name() : BootstrapService.Mode.DeviceInfoMode.name());
        startActivity(intent);
    }

    @Override
    public void updateFinished(List<WirelessNetwork> entries) {
        progressNodes.setVisibility(View.INVISIBLE);
    }

    @Override
    public void updateStarted() {
        progressNodes.setVisibility(View.VISIBLE);
    }

    @Override
    public void onOpenHABDiscovered(OpenHABServer server) {

    }

    @Override
    public void onOpenHABDiscoveryStarted() {
        progressServer.setVisibility(View.VISIBLE);
    }

    @Override
    public void onOpenHABDiscoveryFinished(FinishedState state) {
        progressServer.setVisibility(View.INVISIBLE);
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
