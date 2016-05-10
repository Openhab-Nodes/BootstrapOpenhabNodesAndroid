package org.openhab.detectServers;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.nsd.NsdManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.openhab.bootstrap.R;
import org.openhab.detectServers.adapter.OpenHABAdapter;
import org.openhab.detectServers.adapter.onSelectionChange;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * This activity shows a list of all found OpenHAB servers in the current network (via mDNS) and allows
 * the user to add a custom OpenHAB server (the reachability is checked). If the user selected one server
 * and proceed via the "next" button, the activity given in the manifest file via
 * <meta-data android:name="next_activity" android:value="org.libBSTbootstrap.activities.DetectAndBindActivity" />
 * for example is started. The selected server, username and password will be forwarded by extra data in the calling
 * intent.
 */
public class OpenHABActivity extends AppCompatActivity implements Toolbar.OnMenuItemClickListener, View.OnClickListener, OpenHABDiscoverListener, onSelectionChange {
    @Bind(R.id.fab) FloatingActionButton fab;

    @Bind(R.id.toolbarOpenhabDiscovery) Toolbar toolbarOpenhabDiscovery;
    @Bind(R.id.servers) RecyclerView serverView;
    @Bind(R.id.serversEmpty) View serversEmpty;
    @Bind(R.id.progressServer) ProgressBar progressServer;

    OpenHABAdapter mServerAdapter;
    OpenHABDiscover mOpenHABDiscover;
    private String destClassName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_with_list);
        ButterKnife.bind(this);

        try {
            ApplicationInfo ai = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
            Bundle bundle = ai.metaData;
            destClassName = bundle.getString("next_activity");
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        if (destClassName == null || destClassName.isEmpty())
            throw new RuntimeException("Provide next_activity in your manifest file for the Openhab activity like this: <meta-data android:name=\"next_activity\" android:value=\"...\" />");

        toolbarOpenhabDiscovery.inflateMenu(R.menu.menu_detect);
        toolbarOpenhabDiscovery.setOnMenuItemClickListener(this);

        fab.setOnClickListener(this);

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
        if (requestCode == OpenHABDiscover.REQUEST_LOCATION)
            mOpenHABDiscover.startDiscoveryWithPermissions(this, serverView);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mOpenHABDiscover.stopDiscovery(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mOpenHABDiscover.startDiscoveryWithPermissions(this, serverView);
    }

    void checkConditions() {
        if (mServerAdapter == null)
            return;

        boolean ok = mServerAdapter.getSelected() != null;
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
        Intent intent = new Intent();
        intent.setClassName(this, destClassName);
        intent.putExtra("openhab_server", mServerAdapter.getSelected().host);
        intent.putExtra("openhab_user", mServerAdapter.getSelected().username);
        intent.putExtra("openhab_password", mServerAdapter.getSelected().password);
        startActivity(intent);
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
}
