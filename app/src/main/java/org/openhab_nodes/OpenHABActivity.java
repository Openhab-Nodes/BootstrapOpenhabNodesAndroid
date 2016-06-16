package org.openhab_nodes;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Window;
import android.widget.ProgressBar;

import org.libbootstrapiotdevice.BootstrapData;
import org.openhab.detectServers.OpenHABServer;
import org.openhab.detectServers.OpenHabActivityUtils;
import org.openhab.detectServers.adapter.Adapter;
import org.openhab.detectServers.adapter.onSelectionChange;
import org.openhab_nodes.bootstrap.R;

import de.duenndns.ssl.MemorizingTrustManager;

/**
 * This activity shows a list of all found OpenHAB servers in the current network (via mDNS) and allows
 * the user to add a custom OpenHAB server (the reachability is checked).
 */
public class OpenHABActivity extends AppCompatActivity implements onSelectionChange {
    Toolbar toolbar;
    RecyclerView list;
    private OpenHabActivityUtils openHabActivityUtils = new OpenHabActivityUtils();
    private View btnNext;
    private MemorizingTrustManager memorizingTrustManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            requestWindowFeature(Window.FEATURE_ACTIVITY_TRANSITIONS);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.openhab_server_activity);
        memorizingTrustManager = new MemorizingTrustManager(this);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        list = (RecyclerView) findViewById(R.id.list);
        View emptyView = findViewById(R.id.emptyText);
        ProgressBar progress = (ProgressBar) findViewById(R.id.progress);

        final View openhabImageView = OpenHABActivity.this.findViewById(R.id.openhab_image);
        assert openhabImageView != null;

        View view = findViewById(R.id.btnManually);
        assert view != null;
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OpenHABServer server = new OpenHABServer();
                server.setMemorizingTrustManager(memorizingTrustManager);
                openHabActivityUtils.showOpenHabServerEditDialog(OpenHABActivity.this,
                        openhabImageView, server);
            }
        });

        btnNext = findViewById(R.id.btnNext);
        assert btnNext != null;
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OpenHABServer server = openHabActivityUtils.checkServerAndProceed(OpenHABActivity.this,
                        openhabImageView);
                if (server != null) {
                    BootstrapData.instance().addAdditionalData("openhab_server", server.getUrl());
                    if (server.username != null)
                        BootstrapData.instance().addAdditionalData("openhab_user", server.username);
                    if (server.password != null)
                        BootstrapData.instance().addAdditionalData("openhab_password", server.password);

                    Intent intent = new Intent(OpenHABActivity.this, PrepareBootstrapActivity.class);
                    startActivity(intent);
                }
            }
        });

        setTitle("");

        openHabActivityUtils.createView(this, emptyView, progress);
        Adapter serverAdapter = openHabActivityUtils.getServerAdapter();
        serverAdapter.setOnSelectionChangeListener(this);

        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(serverAdapter);
        list.setItemAnimator(new DefaultItemAnimator());
        list.setHasFixedSize(true);

        onSelectionChanged();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        openHabActivityUtils.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        openHabActivityUtils.stopDiscovery(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        openHabActivityUtils.startDiscovery(this, memorizingTrustManager);
    }

    @Override
    public void onSelectionChanged() {
        btnNext.setEnabled(openHabActivityUtils.getServerAdapter().getSelected() != null);
    }
}
