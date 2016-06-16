package org.libbootstrapiotdevice.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.libbootstrapiotdevice.BootstrapService;
import org.libbootstrapiotdevice.NetworkConnectivityResponse;
import org.libbootstrapiotdevice.R;

public class TestNetworkActivity extends AppCompatActivity implements ServiceConnection, NetworkConnectivityResponse.Callback {
    public static final int REQUEST_CODE = 1462;
    private Button btnOk, btnTest, btnCancel;
    private ProgressBar progressBar;
    private TextView txtWifiName;
    private TextView txtPassword;
    private TextView txtHint;
    private BootstrapService mService;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            requestWindowFeature(Window.FEATURE_ACTIVITY_TRANSITIONS);
        }
        setContentView(R.layout.dialog_test_wifi_credentials);
        btnOk = (Button) findViewById(R.id.btnOk);
        btnTest = (Button) findViewById(R.id.btnTest);
        btnCancel = (Button) findViewById(R.id.btnCancel);
        progressBar = ((ProgressBar) findViewById(R.id.progress));
        txtWifiName = (TextView) findViewById(R.id.wifiname);
        txtPassword = (TextView) findViewById(R.id.password);
        txtHint = (TextView) findViewById(R.id.txtHint);
        assert txtHint != null;
        txtHint.setVisibility(View.GONE);
        progressBar.setVisibility(View.INVISIBLE);

        Intent intent = getIntent();
        txtWifiName.setText(intent != null ? intent.getExtras().getString("wifiname") : "");

        if (txtWifiName.getText().equals("")) {
            throw new RuntimeException("no wifiname set!");
        }

        final SharedPreferences preferences = getSharedPreferences("WifiTestDialog", Context.MODE_PRIVATE);
        txtPassword.setText(preferences.getString("pwd", ""));


        btnOk.setEnabled(false);
        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent result = new Intent();
                result.putExtra("wifipwd", txtPassword.getText().toString());
                setResult(RESULT_OK, result);
                finish();
            }
        });
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });
        btnTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                if (mService == null)
                    return;

                btnTest.setEnabled(false);
                String password = txtPassword.getText().toString();
                preferences.edit().putString("pwd", password).apply();
                progressBar.setIndeterminate(true);
                progressBar.setVisibility(View.VISIBLE);
                mService.testWifi(TestNetworkActivity.this, txtWifiName.getText().toString(), password);
            }
        });
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
    }

    // Called when the connection with the service disconnects unexpectedly
    public void onServiceDisconnected(ComponentName className) {
        mService = null;
    }

    @Override
    public void wifiSuccess(boolean success) {
        btnTest.setEnabled(true);
        progressBar.setVisibility(View.INVISIBLE);
        txtHint.setVisibility(success ? View.GONE : View.VISIBLE);
        btnOk.setEnabled(success);
    }
}
