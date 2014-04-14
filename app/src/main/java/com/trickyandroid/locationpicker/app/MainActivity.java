package com.trickyandroid.locationpicker.app;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import com.squareup.otto.Subscribe;
import com.trickyandroid.locationpicker.app.events.FragmentAnimationStartEvent;
import com.trickyandroid.locationpicker.app.events.ItemSelectedEvent;
import com.trickyandroid.locationpicker.app.events.ListItemSelectedEvent;
import com.trickyandroid.locationpicker.app.events.StartProgressEvent;
import com.trickyandroid.locationpicker.app.fragments.LocationsListFragment;
import com.trickyandroid.locationpicker.app.fragments.LocationsSliderFragment;
import com.trickyandroid.locationpicker.app.geocoding.GeocodingResult;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.Toast;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity implements
        GoogleMap.OnMarkerClickListener, GoogleMap.OnMapLongClickListener,
        GoogleMap.OnMarkerDragListener, GoogleMap.OnMapClickListener {

    private static final String LOCATIONS_LIST_FRAGMENT_TAG = "locations_list_fragment";

    private static final String LOCATIONS_SLIDER_FRAGMENT_TAG = "locations_slider_fragment";

    private static final String CONTENT_EXTRA = "content_extra";

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.

    private ProgressBar progressBar;

    private SearchView searchView;

    private Location lastKnownLocation;

    private final float DEFAULT_ZOOM = 15;

    private final float MIN_ZOOM = 10;

    private Marker selectedLocationMarker = null;

    private ArrayList<Marker> resultMarkers = new ArrayList<Marker>();

    private GeocoderTask ongoingGeocodingTask;

    private MenuItem showAsListMenuItem = null;

    private boolean restoreState = false;

    private boolean animateToCurrentLocation = true;

    private List<GeocodingResult> content = null;

    private Drawable abBackgroundDrawable = null;

    private static final int AB_BACKGROUND_OPACITY = 0xCC;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.abBackgroundDrawable = new ColorDrawable(
                Color.argb(255, 255, 255, 255));
        this.abBackgroundDrawable.setAlpha(AB_BACKGROUND_OPACITY);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            this.abBackgroundDrawable.setCallback(this.drawableCallback);
        } else {
            getActionBar().setBackgroundDrawable(abBackgroundDrawable);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setUpMapIfNeeded();

        getActionBar().setDisplayShowCustomEnabled(true);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setDisplayUseLogoEnabled(true);
        getActionBar().setCustomView(R.layout.ab_layout);
        getActionBar().setBackgroundDrawable(abBackgroundDrawable);

        this.progressBar = (ProgressBar) findViewById(R.id.progressBar);
        this.progressBar.getViewTreeObserver()
                .addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        progressBar.getViewTreeObserver().removeOnPreDrawListener(this);
                        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) progressBar
                                .getLayoutParams();
                        lp.topMargin -= getResources()
                                .getDimension(R.dimen.progressbar_negative_top_margin);
                        progressBar.setLayoutParams(lp);
                        return true;
                    }
                });

        if (savedInstanceState != null && savedInstanceState.containsKey(CONTENT_EXTRA)) {
            this.content = Arrays.asList((GeocodingResult[]) savedInstanceState
                    .getParcelableArray(CONTENT_EXTRA));
            restoreState = true;
            animateToCurrentLocation = false;
        }

        if (savedInstanceState == null && getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_PORTRAIT) {
            getWindow()
                    .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (this.content != null && !this.content.isEmpty()) {
            outState.putParcelableArray(CONTENT_EXTRA,
                    this.content.toArray(new GeocodingResult[this.content.size()]));
        }

        super.onSaveInstanceState(outState);
    }

    @Subscribe
    public void itemSelected(ItemSelectedEvent event) {
        LatLng latLng = event.getItem().getPosition();

        if (selectedLocationMarker == null) {
            MarkerOptions options = new MarkerOptions();
            options.position(latLng);
            options.icon(BitmapDescriptorFactory.defaultMarker());
            options.draggable(true);
            selectedLocationMarker = mMap.addMarker(options);
        } else {
            selectedLocationMarker.setPosition(latLng);
        }

        float zoom = mMap.getCameraPosition().zoom;
        if (zoom < MIN_ZOOM) {
            zoom = DEFAULT_ZOOM;
        }
        animateToCurrentLocation = false;
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng
                , zoom
        ));
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
        MainApplication.getInstance().getBus().register(this);
        if (restoreState) {
            displayResultsOnMap(content);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        MainApplication.getInstance().getBus().unregister(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        searchView = (SearchView) getActionBar().getCustomView().findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new OnSearchViewQueryTextListener());
        showAsListMenuItem = menu.findItem(R.id.list_action_item);
        if (content != null && content.size() > 1) {
            showAsListMenuItem.setVisible(true);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.list_action_item) {

            Fragment listFragmnet = getFragmentManager()
                    .findFragmentByTag(LOCATIONS_LIST_FRAGMENT_TAG);
            if (listFragmnet != null) {
                getFragmentManager().popBackStack();
            } else {
                this.searchView.clearFocus();
                FragmentTransaction t = getFragmentManager().beginTransaction();
                t.add(
                        R.id.locationsListView,
                        LocationsListFragment
                                .getInstance(
                                        content)
                        , LOCATIONS_LIST_FRAGMENT_TAG
                );
                t.addToBackStack(LOCATIONS_LIST_FRAGMENT_TAG);
                t.commit();
            }
            return true;
        } else if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa. <p> This should only be called once and when we are sure that
     * {@link #mMap} is not null.
     */
    private void setUpMap() {
        mMap.setMyLocationEnabled(true);
        mMap.setOnMyLocationChangeListener(new GoogleMap.OnMyLocationChangeListener() {
            @Override
            public void onMyLocationChange(Location location) {
                lastKnownLocation = location;
                if (!animateToCurrentLocation) {
                    return;
                }
                float zoom = mMap.getCameraPosition().zoom;
                mMap.setOnMyLocationChangeListener(null);
                if (zoom < MIN_ZOOM) {
                    zoom = DEFAULT_ZOOM;
                }
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                        new LatLng(location.getLatitude(), location.getLongitude()),
                        zoom));
            }
        });
        mMap.setOnMarkerClickListener(this);
        mMap.setOnMapLongClickListener(this);
        mMap.setOnMarkerDragListener(this);
        mMap.setOnMapClickListener(this);
        updateMapPadding();

        Location currentLocation = mMap.getMyLocation();

        if (currentLocation != null) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()),
                    DEFAULT_ZOOM));
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        if (!TextUtils.isEmpty(marker.getSnippet())) {
            int pos = Integer.valueOf(marker.getSnippet());
            MainApplication.getInstance().getBus()
                    .post(new ListItemSelectedEvent(pos, content.get(pos)));
            return true;
        }
        return false;
    }

    private void requestThroughGeocoder(String query) {
        this.progressBar.setVisibility(View.VISIBLE);

        if (this.ongoingGeocodingTask != null) {
            this.ongoingGeocodingTask.cancel(true);
            this.ongoingGeocodingTask = null;
        }
        this.ongoingGeocodingTask = new GeocoderTask(this, lastKnownLocation);
        this.ongoingGeocodingTask.execute(query);
    }

    private void requestThroughGeocoder(LatLng position) {
        MainApplication.getInstance().getBus().post(new StartProgressEvent(true));
        if (this.ongoingGeocodingTask != null) {
            this.ongoingGeocodingTask.cancel(true);
            this.ongoingGeocodingTask = null;
            this.progressBar.setVisibility(View.GONE);
        }
        this.ongoingGeocodingTask = new GeocoderTask(this, lastKnownLocation);
        this.ongoingGeocodingTask.execute(position);
    }

    private void updateResults(List<GeocodingResult> results) {
        this.progressBar.setVisibility(View.GONE);
        MainApplication.getInstance().getBus().post(new StartProgressEvent(false));
        this.content = results;
        int size = 0;

        if (results == null || results.isEmpty()) {
            Toast.makeText(this, "No results :(", Toast.LENGTH_SHORT).show();
        } else {
            size = results.size();
        }

        showAsListMenuItem.setVisible(size > 1);
        updateLocationsSlider();

        //TODO: move to separate fragment
        displayResultsOnMap(results);
    }

    private void updateLocationsSlider() {
        LocationsSliderFragment fragment = (LocationsSliderFragment) getFragmentManager()
                .findFragmentByTag(LOCATIONS_SLIDER_FRAGMENT_TAG);
        if (content == null || content.isEmpty()) {
            if (fragment != null) {
                getFragmentManager().beginTransaction().remove(fragment).commit();
            }
        } else {
            if (fragment != null) {
                fragment.updateContent(content);
            } else {
                FragmentTransaction t = getFragmentManager().beginTransaction();
                t.add(R.id.locationsSliderFragment,
                        LocationsSliderFragment.getInstance(content),
                        LOCATIONS_SLIDER_FRAGMENT_TAG);
                t.commit();
            }
        }
    }

    private void displayResultsOnMap(List<GeocodingResult> locations) {
        if (locations == null) {
            return;
        }
        MarkerOptions opts = new MarkerOptions();
        opts.anchor(.5f, .5f);
        opts.draggable(false);
        opts.icon(BitmapDescriptorFactory.fromResource(R.drawable.map_icon_pin_mini_map));
        int i = 0;
        for (GeocodingResult l : locations) {
            opts.position(l.getPosition());
            opts.snippet(String.valueOf(i++));
            resultMarkers.add(mMap.addMarker(opts));
        }
    }

    private void removeResultMarkers() {
        for (Marker marker : resultMarkers) {
            marker.remove();
        }
        resultMarkers.clear();
    }

    private void removeSelectedMarker() {
        if (selectedLocationMarker != null) {
            selectedLocationMarker.remove();
            selectedLocationMarker = null;
        }
    }

    /**
     * Remove previous results from the map and hide view pager
     */
    private void clearScreen() {
        content = null;
        removeResultMarkers();
        removeSelectedMarker();
        updateLocationsSlider();
        if (getFragmentManager().getBackStackEntryCount() > 0) {
            getFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);
        updateMapPadding();

        if (fragment.getTag() != null && fragment.getTag().equals(LOCATIONS_LIST_FRAGMENT_TAG)) {
            int alpha = 0xFF;
            ObjectAnimator animator = ObjectAnimator
                    .ofInt(abBackgroundDrawable, "alpha", alpha);
            animator.setDuration(300);
            animator.start();
        }
    }

    @Subscribe
    public void onFragmentAnimationStart(FragmentAnimationStartEvent event) {
        if (event.getFragment().getTag() != null && event.getFragment().getTag()
                .equals(LOCATIONS_LIST_FRAGMENT_TAG)) {
            int alpha = event.isEnter() ? 0xFF : AB_BACKGROUND_OPACITY;
            ObjectAnimator animator = ObjectAnimator
                    .ofInt(abBackgroundDrawable, "alpha", alpha);
            animator.setDuration(300);
            animator.start();
        }
    }

    private void updateMapPadding() {
        int l = 0;
        int t = 0;
        int r = 0;
        int b = 0;
        if (mMap == null) {
            return;
        }
        TypedValue val = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.actionBarSize, val, true);
        t = TypedValue.complexToDimensionPixelSize(val.data,
                getResources().getDisplayMetrics());

        Fragment sliderFragment = getFragmentManager()
                .findFragmentByTag(LOCATIONS_SLIDER_FRAGMENT_TAG);
        if (sliderFragment != null && sliderFragment.isAdded()) {
            b = (int) getResources().getDimension(R.dimen.view_pager_height);
        }
        mMap.setPadding(l, t, r, b);
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        if (selectedLocationMarker == null) {
            MarkerOptions options = new MarkerOptions();
            options.position(latLng);
            options.icon(BitmapDescriptorFactory.defaultMarker());
            options.draggable(true);
            selectedLocationMarker = mMap.addMarker(options);
        } else {
            selectedLocationMarker.setPosition(latLng);
        }
        this.searchView.clearFocus();
        removeResultMarkers();
        requestThroughGeocoder(latLng);
    }

    @Override
    public void onMarkerDragStart(Marker marker) {

    }

    @Override
    public void onMarkerDrag(Marker marker) {

    }

    @Override
    public void onMarkerDragEnd(Marker marker) {
        if (marker.equals(selectedLocationMarker)) {
            this.searchView.clearFocus();
            removeResultMarkers();
            requestThroughGeocoder(marker.getPosition());
        }
    }

    @Override
    public void onMapClick(LatLng latLng) {
        this.searchView.clearFocus();
    }

    class OnSearchViewQueryTextListener implements SearchView.OnQueryTextListener {

        @Override
        public boolean onQueryTextSubmit(String query) {
            clearScreen();
            requestThroughGeocoder(query);
            searchView.clearFocus();
            return true;
        }

        @Override
        public boolean onQueryTextChange(String newText) {
            return false;
        }
    }

    private static class GeocoderTask extends AsyncTask<Object, Void, List<GeocodingResult>> {

        private WeakReference<MainActivity> activity;

        private final int MAX_RESULTS = 20;

        private Location currentLocation;

        GeocoderTask(MainActivity activity, Location currentLocation) {
            this.activity = new WeakReference<MainActivity>(activity);
            this.currentLocation = currentLocation;
        }

        @Override
        protected List<GeocodingResult> doInBackground(Object... params) {
            MainActivity mainActivity = activity.get();
            if (mainActivity == null) {
                return null;
            }
            Geocoder geocoder = new Geocoder(mainActivity, Locale.getDefault());
            List<Address> addresses = null;

            try {
                if (params[0] instanceof String) {
                    addresses = fromString((String) params[0], geocoder);
                } else if (params[0] instanceof LatLng) {
                    addresses = fromLatLng((LatLng) params[0], geocoder);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

            ArrayList<GeocodingResult> results = new ArrayList<GeocodingResult>();
            if (addresses != null) {
                float[] distance = new float[1];
                for (Address a : addresses) {
                    GeocodingResult r = GeocodingResult.fromAddress(a);
                    if (currentLocation != null) {
                        Location.distanceBetween(currentLocation.getLatitude(),
                                currentLocation.getLongitude(), r.getPosition().latitude,
                                r.getPosition().longitude, distance);
                        r.setDistance(distance[0]);
                    }
                    results.add(r);
                }
                Collections.sort(results);
            }

            return results;
        }

        private List<Address> fromString(String query, Geocoder geocoder) throws IOException {
            List<Address> addresses = null;
            if (currentLocation != null) {
                int radius = 2;
                double lat_bot = currentLocation.getLatitude() - radius;
                double lat_top = currentLocation.getLatitude() + radius;
                double lon_left = currentLocation.getLongitude() - radius;
                double lon_right = currentLocation.getLongitude() + radius;

                addresses = geocoder
                        .getFromLocationName(query,
                                MAX_RESULTS, lat_bot, lon_left, lat_top, lon_right);
            }
            if (addresses == null || addresses.isEmpty()) {
                addresses = geocoder
                        .getFromLocationName(query,
                                MAX_RESULTS);
            }
            return addresses;
        }

        private List<Address> fromLatLng(LatLng position, Geocoder geocoder) throws IOException {
            return geocoder.getFromLocation(position.latitude, position.longitude, 1);
        }


        @Override
        protected void onPostExecute(List<GeocodingResult> results) {
            super.onPostExecute(results);
            if (activity.get() != null && !isCancelled()) {
                activity.get().updateResults(results);
            }
        }
    }

    private Drawable.Callback drawableCallback = new Drawable.Callback() {
        @Override
        public void invalidateDrawable(Drawable who) {
            getActionBar().setBackgroundDrawable(who);
        }

        @Override
        public void scheduleDrawable(Drawable who, Runnable what, long when) {

        }

        @Override
        public void unscheduleDrawable(Drawable who, Runnable what) {

        }
    };
}
