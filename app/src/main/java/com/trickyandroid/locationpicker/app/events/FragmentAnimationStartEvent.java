package com.trickyandroid.locationpicker.app.events;

import android.app.Fragment;

/**
 * Created by paveld on 4/12/14.
 */
public class FragmentAnimationStartEvent {

    private Fragment fragment;

    private boolean enter;

    public FragmentAnimationStartEvent(Fragment f, boolean enter) {
        this.fragment = f;
        this.enter = enter;
    }

    public Fragment getFragment() {
        return fragment;
    }

    public boolean isEnter() {
        return enter;
    }
}
