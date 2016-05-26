package org.openhab.detectServers;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import org.openhab.R;

public class EditServerActivity extends AppCompatActivity implements CheckConnectivity.OpenHABConnectivityListener {
    public static final String EXTRA_TRANSITION_IMAGE = "openhab_image";
    public static final int REQUEST_CODE = 123;
    public static OpenHABServer server;
    private Button btnOk, btnTest, btnCancel;
    private ProgressBar progressBar;
    private TextView txtUsername;
    private TextView txtPassword;
    private TextView txtHost;
    private Spinner http_https;
    private TextView txtPort;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            requestWindowFeature(Window.FEATURE_ACTIVITY_TRANSITIONS);
        }
        setContentView(R.layout.edit_server_activity);
        btnOk = (Button) findViewById(R.id.btnOk);
        btnTest = (Button) findViewById(R.id.btnTest);
        btnCancel = (Button) findViewById(R.id.btnCancel);
        progressBar = ((ProgressBar) findViewById(R.id.progress));
        txtUsername = (TextView) findViewById(R.id.username);
        txtPassword = (TextView) findViewById(R.id.password);
        txtHost = (TextView) findViewById(R.id.host);
        txtPort = (TextView) findViewById(R.id.port);
        http_https = (Spinner) findViewById(R.id.secure);

        TextView txtHint = (TextView) findViewById(R.id.txtHint);
        assert txtHint != null;
        txtHint.setVisibility(server.getConnectivity() != OpenHABServer.Connectivity.Reachable && (server.getHost() != null) ?
                View.VISIBLE : View.GONE);

        if (server.username != null) {
            txtUsername.setText(server.username);
        }
        if (server.password != null) {
            txtPassword.setText(server.password);
        }

        final SharedPreferences preferences = getSharedPreferences("OpenHabServerAdd", Context.MODE_PRIVATE);

        if (server.getHost() != null) {
            http_https.setSelection(server.isSecure() ? 0 : 1);
            txtHost.setText(server.getHost());
        } else {
            http_https.setSelection(preferences.getInt("secure", 0));
            txtHost.setText(preferences.getString("host", ""));
        }

        if (server.getPort() == 0) {
            txtPort.setText(preferences.getString("port", "8080"));
        } else
            txtPort.setText(String.valueOf(server.getPort()));


        btnOk.setEnabled(false);
        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setResult(RESULT_OK);
                supportFinishAfterTransition();
            }
        });
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setResult(RESULT_CANCELED);
                supportFinishAfterTransition();
            }
        });
        btnTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                TextInputLayout til;

                String host = txtHost.getText().toString();

                til = (TextInputLayout) findViewById(R.id.txtPortInput);
                assert til != null;

                int port;
                try {
                    til.setErrorEnabled(false);
                    port = Integer.valueOf(txtPort.getText().toString());
                    if (port < 80 || port > 65000)
                        throw new NumberFormatException("Port invalid");
                } catch (NumberFormatException ignored) {
                    til.setErrorEnabled(true);
                    til.setError("Invalid!");
                    port = 0;
                }
                boolean secure = http_https.getSelectedItemPosition() == 0;

                server.setHost(host, port, secure);
                til = (TextInputLayout) findViewById(R.id.txtHostInput);
                assert til != null;
                if (host.trim().isEmpty() || server.getUrl() == null) {
                    til.setErrorEnabled(true);
                    til.setError("Server address invalid");
                    return;
                } else {
                    til.setErrorEnabled(false);
                }

                if (port == 0) {
                    return;
                }

                preferences.edit()
                        .putInt("secure", secure ? 0 : 1)
                        .putString("host", host)
                        .putString("port", txtPort.getText().toString()).apply();

                server.setCredentials(txtUsername.getText().toString(), txtPassword.getText().toString());
                progressBar.setVisibility(View.VISIBLE);
                server.updateConnectivity(0, EditServerActivity.this);
            }
        });
    }

    @Override
    public void openhabConnectivityChanged(@NonNull OpenHABServer server, int index, boolean changed) {
        progressBar.setVisibility(View.INVISIBLE);
        switch (server.getConnectivity()) {
            case Reachable:
                btnOk.setEnabled(true);
                Snackbar.make(btnTest, "Server OK", Snackbar.LENGTH_SHORT).show();
                break;
            case ConnectionError:
            case NotReachable:
                btnOk.setEnabled(false);
                Snackbar.make(btnTest, "Server not found", Snackbar.LENGTH_SHORT).show();
                break;
            case ReachableAccessDenied:
                btnOk.setEnabled(false);
                Snackbar.make(btnTest, "Server access denied", Snackbar.LENGTH_SHORT).show();
                break;
        }
    }
}
