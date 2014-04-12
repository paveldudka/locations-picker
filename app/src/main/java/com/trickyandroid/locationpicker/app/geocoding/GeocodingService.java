package com.trickyandroid.locationpicker.app.geocoding;

import retrofit.Callback;
import retrofit.http.GET;
import retrofit.http.Query;

/**
 * Created by paveld on 4/10/14.
 */
public interface GeocodingService {

    @GET("/maps/api/geocode/json?sensor=true")
    void fromAddress(@Query("address") String address, Callback<GeocodingResult> callback);
}
