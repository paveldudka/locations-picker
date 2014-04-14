package com.trickyandroid.locationpicker.app.fragments;

import com.trickyandroid.locationpicker.app.MainApplication;
import com.trickyandroid.locationpicker.app.R;
import com.trickyandroid.locationpicker.app.events.FragmentAnimationStartEvent;
import com.trickyandroid.locationpicker.app.events.ListItemSelectedEvent;
import com.trickyandroid.locationpicker.app.geocoding.GeocodingResult;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.app.ListFragment;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;

/**
 * Created by paveld on 4/11/14.
 */
public class LocationsListFragment extends ListFragment {

    private final static String CONTENT_EXTRA = "content_extra";

    private final double METERS_IN_MILE = 1609.34;

    private List<GeocodingResult> locations;

    public static LocationsListFragment getInstance(List<GeocodingResult> content) {
        Bundle args = new Bundle();
        args.putParcelableArray(CONTENT_EXTRA,
                content.toArray(new GeocodingResult[content.size()]));
        LocationsListFragment f = new LocationsListFragment();
        f.setArguments(args);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.locations_list_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            this.locations = Arrays
                    .asList((GeocodingResult[]) (getArguments().getParcelableArray(CONTENT_EXTRA)));
        }

        //TODO: move to XML
        setListAdapter(new LocationsListAdapter(locations));
        getListView().setBackgroundColor(0xFFDEDEDE);
        this.getListView().setDivider(
                new ColorDrawable(getResources().getColor(android.R.color.transparent)));
        this.getListView().setScrollBarStyle(ListView.SCROLLBARS_OUTSIDE_OVERLAY);
        this.getListView().setDividerHeight(
                getResources().getDimensionPixelSize(R.dimen.list_separator_height));
        this.getListView().setClipChildren(false);
        this.getListView().setDrawSelectorOnTop(true);

        TypedValue val = new TypedValue();
        getActivity().getTheme()
                .resolveAttribute(android.R.attr.selectableItemBackground, val, true);
        this.getListView().setSelector(val.resourceId);

        int padding = getResources().getDimensionPixelSize(R.dimen.list_padding);
        this.getListView().setPadding(padding, padding, padding, padding);
        this.getListView().setClipToPadding(false);
        this.getListView().requestFocus();
    }

    @Override
    public Animator onCreateAnimator(int transit, boolean enter, int nextAnim) {
        int animResId = enter ? R.animator.slide_up : R.animator.slide_down;
        MainApplication.getInstance().getBus().post(new FragmentAnimationStartEvent(this, enter));
        return AnimatorInflater.loadAnimator(getActivity(), animResId);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        MainApplication.getInstance().getBus()
                .post(new ListItemSelectedEvent(position, locations.get(position)));
        getFragmentManager().popBackStack();
    }

    class LocationsListAdapter extends BaseAdapter {

        private List<GeocodingResult> content;

        LocationsListAdapter(List<GeocodingResult> results) {
            this.content = results;
        }

        @Override
        public int getCount() {
            return content.size();
        }

        @Override
        public GeocodingResult getItem(int position) {
            return content.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View result = convertView;
            if (result == null) {
                result = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.list_item_layout, parent, false);
                ViewHolder vh = new ViewHolder();
                vh.title = (TextView) result.findViewById(R.id.title);
                vh.description = (TextView) result.findViewById(R.id.description);
                vh.distance = (TextView) result.findViewById(R.id.distance);
                result.setTag(vh);
            }

            ViewHolder vh = (ViewHolder) result.getTag();
            vh.title.setText(getItem(position).getTitle());
            vh.description.setText(getItem(position).getDescription());

            NumberFormat format = NumberFormat.getNumberInstance();
            format.setMinimumFractionDigits(0);
            format.setMaximumFractionDigits(1);
            String distanceStr = format.format(getItem(position).getDistance() / METERS_IN_MILE);
            vh.distance.setText(distanceStr);
            return result;
        }

        class ViewHolder {

            TextView title;

            TextView description;

            TextView distance;
        }
    }
}
