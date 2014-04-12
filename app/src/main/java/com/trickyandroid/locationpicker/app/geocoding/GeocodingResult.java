package com.trickyandroid.locationpicker.app.geocoding;

import com.google.android.gms.maps.model.LatLng;

import android.location.Address;
import android.os.Parcel;
import android.os.Parcelable;

public class GeocodingResult implements Comparable<GeocodingResult>, Parcelable {

    private String title;

    private String description;

    private LatLng position;

    private float distance;

    public static GeocodingResult fromAddress(Address address) {
        GeocodingResult result = new GeocodingResult();
        result.setTitle(address.getAddressLine(0));
        result.setDescription(address.getAddressLine(1));
        result.setPosition(new LatLng(address.getLatitude(), address.getLongitude()));
        return result;
    }

    public GeocodingResult() {
    }

    public GeocodingResult(Parcel in) {
        String[] data = new String[5];
        in.readStringArray(data);
        this.title = data[0];
        this.description = data[1];
        this.position = new LatLng(Double.valueOf(data[2]), Double.valueOf(data[3]));
        this.distance = Float.valueOf(data[4]);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LatLng getPosition() {
        return position;
    }

    public void setPosition(LatLng position) {
        this.position = position;
    }

    public float getDistance() {
        return distance;
    }

    public void setDistance(float distance) {
        this.distance = distance;
    }

    @Override
    public int compareTo(GeocodingResult another) {
        return (distance < another.getDistance()) ? -1 : 1;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStringArray(
                new String[]{this.title, this.description, String.valueOf(position.latitude),
                        String.valueOf(position.longitude), String.valueOf(this.distance)}
        );
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public GeocodingResult createFromParcel(Parcel in) {
            return new GeocodingResult(in);
        }

        public GeocodingResult[] newArray(int size) {
            return new GeocodingResult[size];
        }
    };
}


