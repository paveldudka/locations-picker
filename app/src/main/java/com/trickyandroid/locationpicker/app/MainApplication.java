package com.trickyandroid.locationpicker.app;

import com.squareup.otto.Bus;

import android.app.Application;

/**
 * Created by paveld on 4/11/14.
 */
public class MainApplication extends Application {

    private Bus bus;

    private static MainApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        bus = new Bus();
    }

    public Bus getBus() {
        return this.bus;
    }

    public static MainApplication getInstance() {
        return instance;
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        bus = null;
        instance = null;
    }
}
