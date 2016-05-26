package org.libbootstrapiotdevice.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.libbootstrapiotdevice.BootstrapService;
import org.libbootstrapiotdevice.R;
import org.libbootstrapiotdevice.adapter.onSelectionChange;
import org.libbootstrapiotdevice.network.BootstrapDeviceUpdateListener;
import org.libbootstrapiotdevice.network.WifiUtils;

public class DetectAndBindActivity extends AppCompatActivity implements BootstrapDeviceUpdateListener, onSelectionChange, View.OnClickListener, BootstrapActivityUtils.BootstrapServiceReady {
    public static final String EXTRA_IS_LOCAL_NETWORK = "IS_LOCAL_NET";
    private Button btnOK;
    private Button btnRetry;
    private ProgressBar progress;
    private BootstrapActivityUtils utils = new BootstrapActivityUtils();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_with_list);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() == null)
            throw new RuntimeException();

        btnOK = (Button) findViewById(R.id.btnNext);
        btnRetry = (Button) findViewById(R.id.btnRetry);
        progress = (ProgressBar) findViewById(R.id.progress);

        RecyclerView list = (RecyclerView) findViewById(R.id.list);
        TextView emptyText = (TextView) findViewById(R.id.emptyText);
        assert emptyText != null;
        emptyText.setText(R.string.no_devices_found);

        btnOK.setOnClickListener(this);
        btnRetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startProcess();
            }
        });

        utils.onCreate(this, list, emptyText, this);
        utils.getDevicesAdapter().setOnSelectionChangeListener(this);
    }

    @Override
    public void onSelectionChanged() {
        if (utils.getService() == null)
            return;
        btnOK.setEnabled(utils.getDevicesAdapter().isOneSelected());
    }

    // OK Button
    @Override
    public void onClick(View v) {
        if (utils.getService() == null)
            return;

        if (utils.getDevicesAdapter().isOneSelected()) {
            Intent intent = new Intent(this, DestinationNetworkActivity.class);
            startActivity(intent);
        } else {
            Snackbar.make(v, R.string.error_select_one_entry, Snackbar.LENGTH_SHORT).show();
        }
    }


    private void startProcess() {
        btnRetry.setEnabled(false);
        btnOK.setEnabled(false);
        progress.setIndeterminate(true);
        progress.setVisibility(View.VISIBLE);

        final Snackbar snackbar = Snackbar.make(btnOK, R.string.setup_ap, Snackbar.LENGTH_INDEFINITE);

        final BootstrapService service = utils.getService();
        if (!service.isSameNetwork()) {
            if (WifiUtils.checkWifiAP(this, service.getAccessPointSsid(), service.getAccessPointKey())) {
                service.takeOverAP(this);
            } else {
                snackbar.show();
                service.startWifiAP(this, new BootstrapService.Callback() {
                    @Override
                    public void wifiSuccess(boolean success) {
                        if (success) {
                            snackbar.dismiss();
                            if (!service.getBootstrapDevices().detectDevices())
                                deviceChangesFinished();
                        } else {
                            snackbar.dismiss();
                            snackbar.setText(R.string.ap_setup_failed);
                            snackbar.setDuration(Snackbar.LENGTH_SHORT);
                            snackbar.show();
                            deviceChangesFinished();
                        }
                    }
                });
                return;
            }
        }

        if (!service.getBootstrapDevices().detectDevices())
            deviceChangesFinished();
    }

    @Override
    public void deviceUpdated(int index, boolean added) {
    }

    @Override
    public void deviceRemoved(int index) {
    }

    @Override
    public void deviceRemoveAll() {
    }

    @Override
    public void deviceChangesFinished() {
        progress.setVisibility(View.INVISIBLE);
        btnOK.setEnabled(true);
        btnRetry.setEnabled(true);
    }

    @Override
    protected void onStop() {
        unbindService(utils);
        utils.getService().restoreWifi(this);
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, BootstrapService.class);
        bindService(intent, utils, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onBootstrapServiceReady() {
        utils.getService().getBootstrapDevices().addChangeListener(this);
        startProcess();
    }
}
