package org.openhab_nodes;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.TextView;

import org.libbootstrapiotdevice.BootstrapService;
import org.libbootstrapiotdevice.activities.DetectAndBindActivity;
import org.libbootstrapiotdevice.network.WifiUtils;
import org.openhab_nodes.bootstrap.R;

/**
 * Shows an activity with a text, a help button and a next button.
 * You have to customize these with via the calling intent and
 * data in the extra bundle. Specify the following extras:
 * - next_activity (string): The class.getName() class identifier
 * for the activity that should be opened
 * if the user presses next.
 * - text_res_id (int):      The res id of a text that should be displayed.
 * - wiki_help_id (string):  The last part of the help url which usually
 * points to a subpage of a wiki.
 * Example:
 * Intent intent = IntroductionActivity.createIntroductionIntent(this,
 * R.string.text, "wikipage", SomeActivity.class.getName());
 * startActivity(intent);
 */
public class PrepareBootstrapActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener, View.OnClickListener, ServiceConnection, BootstrapService.Callback {
    protected BootstrapService mService;
    TextView txtAccessPoint;
    TextView txtAndroid6Permission;
    RadioButton radioIsLocal;
    Button btnNext;
    Snackbar snackbar;
    private TextView txtAccessPointSetupFailed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.prepare_activity);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        txtAccessPoint = (TextView) findViewById(R.id.txtAccessPoint);
        txtAndroid6Permission = (TextView) findViewById(R.id.txtAndroid6Permission);
        txtAccessPointSetupFailed = (TextView) findViewById(R.id.txtAccessPointSetupFailed);
        radioIsLocal = (RadioButton) findViewById(R.id.radioLocalNetwork);

        assert radioIsLocal != null;

        radioIsLocal.setOnCheckedChangeListener(this);
        radioIsLocal.setChecked(true);

        txtAccessPointSetupFailed.setVisibility(View.GONE);

        btnNext = (Button) findViewById(R.id.btnNext);
        assert btnNext != null;
        btnNext.setOnClickListener(this);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        assert fab != null;
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO repair help url
                Intent i = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("http://github.com/Openhab-Nodes/android/wiki/prepare_bootstrap"));
                startActivity(i);
            }
        });
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean networkIsLocal) {
        if (networkIsLocal) {
            txtAccessPoint.setVisibility(View.GONE);
            txtAndroid6Permission.setVisibility(View.GONE);
        } else {
            txtAccessPoint.setVisibility(View.VISIBLE);
            boolean needPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && !mService.checkPermissions();
            txtAndroid6Permission.setVisibility(needPermission ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mService != null)
            btnNext.setText(mService.checkPermissions() ? R.string.btnNext : R.string.btnNeedPermission);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, BootstrapService.class);
        bindService(intent, this, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        mService.restoreWifi(this);
        unbindService(this);
        super.onStop();
    }

    // Next button
    @Override
    public void onClick(View v) {
        if (mService == null) {
            Snackbar.make(v, R.string.service_not_ready, Snackbar.LENGTH_LONG).show();
            return;
        }
        if (!WifiUtils.checkWifiAP(this, mService.getAccessPointSsid(), mService.getAccessPointKey())) {
            if (!mService.checkPermissions()) {
                mService.openPermissionSettings(this);
                return;
            }
            btnNext.setEnabled(false);
            mService.startWifiAP(this, this);
            snackbar = Snackbar.make(v, org.libbootstrapiotdevice.R.string.setup_ap, Snackbar.LENGTH_INDEFINITE);
            snackbar.show();
            return;
        }

        mService.setSameNetwork(radioIsLocal.isChecked());
        Intent intent = new Intent(PrepareBootstrapActivity.this, DetectAndBindActivity.class);
        startActivity(intent);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        BootstrapService.LocalBinder binder = (BootstrapService.LocalBinder) service;
        mService = binder.getService();
        btnNext.setText(mService.checkPermissions() ? R.string.btnNext : R.string.btnNeedPermission);
        txtAccessPointSetupFailed.setText(getString(R.string.txtFailedSetupAP, mService.getAccessPointSsid(), mService.getAccessPointKey()));
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        mService = null;
    }

    @Override
    public void wifiSuccess(boolean success) {
        btnNext.setEnabled(true);
        snackbar.dismiss();
        snackbar = null;

        if (success) {
            Intent intent = new Intent(PrepareBootstrapActivity.this, DetectAndBindActivity.class);
            intent.putExtra(DetectAndBindActivity.EXTRA_IS_LOCAL_NETWORK, radioIsLocal.isChecked());
            startActivity(intent);
        } else {
            txtAccessPointSetupFailed.setVisibility(View.VISIBLE);
        }
    }
}
