package com.trickyandroid.locationpicker.app.fragments;

import com.trickyandroid.locationpicker.app.MainApplication;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by paveld on 4/12/14.
 */
public class LocationsSliderFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        MainApplication.getInstance().getBus().register(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        MainApplication.getInstance().getBus().unregister(this);
    }
}
