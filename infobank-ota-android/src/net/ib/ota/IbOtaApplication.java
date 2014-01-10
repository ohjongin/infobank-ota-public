package net.ib.ota;

import android.app.Application;

import com.parse.Parse;
import com.parse.ParseACL;

import net.ib.ota.util.Log;

/**
 * Created by ohjongin on 13. 7. 8.
 */
public class IbOtaApplication extends Application {
    private static final String APPLICATION_ID = "";
    private static final String CLIENT_KEY = "";

    @Override
    public void onCreate() {
        super.onCreate();
        Parse.initialize(this, APPLICATION_ID, CLIENT_KEY);


        ParseACL defaultACL = new ParseACL();
        // Optionally enable public read access.
        defaultACL.setPublicReadAccess(true);
        ParseACL.setDefaultACL(defaultACL, true);

        Log.setLogTag("IbOta");
        Log.setDebugMode(true);
    }

}
