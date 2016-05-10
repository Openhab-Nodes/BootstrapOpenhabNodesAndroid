package org.openhab.bootstrap;

import android.app.Application;
import android.os.Bundle;

import org.libBSTbootstrap.BootstrapDataIntentExtractor;
import org.libBSTbootstrap.BootstrapService;

import java.util.Map;
import java.util.TreeMap;

/**
 * Created by david on 10.05.16.
 */
public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        BootstrapService.addDataIntentExtractor(new BootstrapDataIntentExtractor() {
            @Override
            public Map<String, String> getData(Bundle extra) {
                Map<String, String> data = new TreeMap<>();
                if (extra != null && extra.containsKey("openhab_server")) {
                    data.put("openhab_server", extra.getString("openhab_server"));
                    data.put("openhab_user", extra.getString("openhab_user"));
                    data.put("openhab_password", extra.getString("openhab_password"));
                }
                return data;
            }
        });
    }
}
