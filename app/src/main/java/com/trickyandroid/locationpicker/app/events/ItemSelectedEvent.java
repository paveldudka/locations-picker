package com.trickyandroid.locationpicker.app.events;

import com.trickyandroid.locationpicker.app.geocoding.GeocodingResult;

/**
 * Created by paveld on 4/11/14.
 */
public class ItemSelectedEvent {

    private int position;

    private GeocodingResult item;

    public ItemSelectedEvent(int position, GeocodingResult item) {
        this.position = position;
        this.item = item;
    }

    public int getPosition() {
        return position;
    }

    public GeocodingResult getItem() {
        return item;
    }
}
