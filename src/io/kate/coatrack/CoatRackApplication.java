package io.kate.coatrack;

import android.app.Application;

public class CoatRackApplication extends Application {
    public static final String PREFS = "ssf_prefs";
    public static final String PREF_SERVER_ADDRESS = "server_address";
    public static final String PREF_SERVER_PORT = "server_port";
    public static final int DEFAULT_PORT = 2000;
    public static final String DEFAULT_IP = "192.168.43.212";

    CoatRackApplication instance;
    
    public CoatRackApplication getInstance() {
        if (instance == null) instance = this;
        return instance;
    }
}
