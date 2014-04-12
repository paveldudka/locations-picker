package com.trickyandroid.locationpicker.app.events;

/**
 * Created by paveld on 4/11/14.
 */
public class ListItemSelectedEvent {

    private int position;

    public ListItemSelectedEvent(int position) {
        this.position = position;
    }

    public int getPosition() {
        return position;
    }
}
