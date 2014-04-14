package com.trickyandroid.locationpicker.app.fragments;

import com.squareup.otto.Produce;
import com.squareup.otto.Subscribe;
import com.trickyandroid.locationpicker.app.MainApplication;
import com.trickyandroid.locationpicker.app.R;
import com.trickyandroid.locationpicker.app.events.FragmentAnimationStartEvent;
import com.trickyandroid.locationpicker.app.events.ItemSelectedEvent;
import com.trickyandroid.locationpicker.app.events.ListItemSelectedEvent;
import com.trickyandroid.locationpicker.app.events.StartProgressEvent;
import com.trickyandroid.locationpicker.app.geocoding.GeocodingResult;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.List;

/**
 * Created by paveld on 4/12/14.
 */
public class LocationsSliderFragment extends Fragment implements ViewPager.OnPageChangeListener {

    private static final String CONTENT_EXTRA = "content_extra";

    private static final String CURRENT_SELECTION_EXTRA = "current_selection_extra";

    private ViewPager viewPager;

    private ProgressBar progressBar;

    private List<GeocodingResult> content;

    public static LocationsSliderFragment getInstance(List<GeocodingResult> content) {
        LocationsSliderFragment f = new LocationsSliderFragment();
        Bundle args = new Bundle();

        GeocodingResult[] array = content.toArray(new GeocodingResult[content.size()]);
        args.putParcelableArray(CONTENT_EXTRA, array);
        f.setArguments(args);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.locations_slider_fragment, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.viewPager = (ViewPager) view.findViewById(R.id.viewPager);
        this.viewPager.setOnPageChangeListener(this);
        this.progressBar = (ProgressBar) view.findViewById(R.id.viewPagerProgressBar);
        if (getArguments() != null) {
            this.content = Arrays
                    .asList((GeocodingResult[]) getArguments().getParcelableArray(CONTENT_EXTRA));
            updateContent(this.content, getArguments().getInt(CURRENT_SELECTION_EXTRA));
        }
    }

    @Produce
    public ItemSelectedEvent produceItemSelectedEvent() {
        if (content == null) {
            return null;
        }
        return new ItemSelectedEvent(viewPager.getCurrentItem(),
                content.get(viewPager.getCurrentItem()));
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        MainApplication.getInstance().getBus().register(this);
    }

    @Override
    public Animator onCreateAnimator(int transit, boolean enter, int nextAnim) {
        int animResId = enter ? R.animator.slide_up : R.animator.slide_down;
        MainApplication.getInstance().getBus().post(new FragmentAnimationStartEvent(this, enter));
        return AnimatorInflater.loadAnimator(getActivity(), animResId);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        MainApplication.getInstance().getBus().unregister(this);
    }

    public void updateContent(List<GeocodingResult> newContent) {
        updateContent(newContent, 0);
    }

    public void updateContent(List<GeocodingResult> newContent, int initialPosition) {
        this.content = newContent;
        this.viewPager.setAdapter(new ViewPagerAdapter(newContent));
        this.viewPager.setCurrentItem(initialPosition);
        onPageSelected(viewPager.getCurrentItem());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        getArguments().putParcelableArray(CONTENT_EXTRA,
                content.toArray(new GeocodingResult[content.size()]));
        getArguments().putInt(CURRENT_SELECTION_EXTRA, this.viewPager.getCurrentItem());
    }

    @Subscribe
    public void onStartProgress(StartProgressEvent event) {
        if (event.isStart()) {
            this.progressBar.setVisibility(View.VISIBLE);
            this.content = null;
            this.viewPager.setAdapter(null);
        } else {
            this.progressBar.setVisibility(View.GONE);
        }
    }

    @Subscribe
    public void listItemSelected(ListItemSelectedEvent event) {
        this.viewPager.setCurrentItem(event.getPosition(), true);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        MainApplication.getInstance().getBus()
                .post(new ItemSelectedEvent(position, content.get(position)));
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    private class ViewPagerAdapter extends PagerAdapter {

        private List<GeocodingResult> locations;

        public ViewPagerAdapter(List<GeocodingResult> locations) {
            this.locations = locations;
        }

        @Override
        public int getCount() {
            if (locations == null) {
                return 0;
            }
            return locations.size();
        }

        public List<GeocodingResult> getContent() {
            return this.locations;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View result = LayoutInflater.from(container.getContext())
                    .inflate(R.layout.sliding_item_layout, container, false);
            ((TextView) result.findViewById(R.id.title))
                    .setText(locations.get(position).getTitle());
            ((TextView) result.findViewById(R.id.description))
                    .setText(locations.get(position).getDescription());
            container.addView(result);
            result.findViewById(R.id.saveImg).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(v.getContext(), "Congratulations! You just saved this place!",
                            Toast.LENGTH_LONG).show();
                }
            });
            return result;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }
    }
}
