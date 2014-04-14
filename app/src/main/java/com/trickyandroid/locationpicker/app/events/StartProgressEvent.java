package com.trickyandroid.locationpicker.app.events;

/**
 * Created by paveld on 4/12/14.
 */
public class StartProgressEvent {

    private boolean start;

    public StartProgressEvent(boolean start) {
        this.start = start;
    }

    public boolean isStart() {
        return start;
    }
}
