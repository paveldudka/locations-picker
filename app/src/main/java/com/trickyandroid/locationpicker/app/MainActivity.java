package com.trickyandroid.locationpicker.app;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import com.squareup.otto.Subscribe;
import com.trickyandroid.locationpicker.app.events.ListItemSelectedEvent;
import com.trickyandroid.locationpicker.app.fragments.LocationsListFragment;
import com.trickyandroid.locationpicker.app.geocoding.GeocodingResult;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.res.Configuration;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity implements ViewPager.OnPageChangeListener,
        GoogleMap.OnMarkerClickListener, GoogleMap.OnMapLongClickListener,
        GoogleMap.OnMarkerDragListener, GoogleMap.OnMapClickListener {

    private static final String LOCATIONS_LIST_FRAGMENT_TAG = "locations_list_fragment";

    private static final String CONTENT_EXTRA = "content_extra";

    private static final String CURRENT_SELECTION_EXTRA = "current_selection_extra";

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.

    private ProgressBar progressBar;

    private ProgressBar viewPagerProgressBar;

    private SearchView searchView;

    private Location lastKnownLocation;

    private ViewPager viewPager;

    private ViewGroup viewPagerContainer;

    private final float DEFAULT_ZOOM = 15;

    private final float MIN_ZOOM = 10;

    private Marker selectedLocationMarker = null;

    private ArrayList<Marker> resultMarkers = new ArrayList<Marker>();

    private GeocoderTask ongoingGeocodingTask;

    private MenuItem showAsListMenuItem = null;

    private boolean restoreState = false;

    private boolean animateToCurrentLocation = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewPagerContainer = (ViewGroup) findViewById(R.id.viewPagerContainer);
        viewPager = (ViewPager) findViewById(R.id.viewPager);
        if (savedInstanceState != null && savedInstanceState.containsKey(CONTENT_EXTRA)) {
            List<GeocodingResult> content = Arrays.asList((GeocodingResult[]) savedInstanceState
                    .getParcelableArray(CONTENT_EXTRA));
            viewPager.setAdapter(new ViewPagerAdapter(content));
            viewPager.setCurrentItem(savedInstanceState.getInt(CURRENT_SELECTION_EXTRA), false);
            viewPagerContainer.setVisibility(View.VISIBLE);
            restoreState = true;
        }

        viewPager.setOnPageChangeListener(this);

        setUpMapIfNeeded();

        getActionBar().setDisplayShowCustomEnabled(true);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setDisplayUseLogoEnabled(true);
        getActionBar().setCustomView(R.layout.ab_layout);

        this.viewPagerProgressBar = (ProgressBar) findViewById(R.id.viewPagerProgressBar);
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

        if (savedInstanceState == null && getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_PORTRAIT) {
            getWindow()
                    .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        MainApplication.getInstance().getBus().register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        MainApplication.getInstance().getBus().unregister(this);
    }

    @Subscribe
    public void answerAvailable(ListItemSelectedEvent event) {
        this.viewPager.setCurrentItem(event.getPosition(), true);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset,
            int positionOffsetPixels) {
    }

    @Override
    public void onPageSelected(int position) {
        LatLng latLng = ((ViewPagerAdapter) viewPager.getAdapter()).getLocation(position)
                .getPosition();

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
    public void onPageScrollStateChanged(int state) {

    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
        if (restoreState) {
            displayResultsOnMap(((ViewPagerAdapter) viewPager.getAdapter()).getContent());
            onPageSelected(viewPager.getCurrentItem());
            restoreState = false;
            updateMapPadding();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        searchView = (SearchView) getActionBar().getCustomView().findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new OnSearchViewQueryTextListener());
        showAsListMenuItem = menu.findItem(R.id.list_action_item);
        if (viewPager.getAdapter() != null) {
            showAsListMenuItem.setVisible(viewPager.getAdapter().getCount() > 1);
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
                getFragmentManager().beginTransaction().add(R.id.locationsListView,
                        LocationsListFragment
                                .getInstance(
                                        ((ViewPagerAdapter) viewPager.getAdapter()).getContent())
                        , LOCATIONS_LIST_FRAGMENT_TAG
                )
                        .addToBackStack(null).commit();
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
            viewPager.setCurrentItem(Integer.valueOf(marker.getSnippet()), true);
            return true;
        }
        return false;
    }

    private void requestThroughGeocoder(String query) {
        this.progressBar.setVisibility(View.VISIBLE);

        if (this.ongoingGeocodingTask != null) {
            this.ongoingGeocodingTask.cancel(true);
            this.ongoingGeocodingTask = null;
            this.viewPagerProgressBar.setVisibility(View.GONE);
        }
        this.ongoingGeocodingTask = new GeocoderTask(this, lastKnownLocation);
        this.ongoingGeocodingTask.execute(query);
    }

    private void requestThroughGeocoder(LatLng position) {
        this.viewPagerProgressBar.setVisibility(View.VISIBLE);
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
        this.viewPagerProgressBar.setVisibility(View.GONE);

        if (results == null) {
            Toast.makeText(this, "Error :(", Toast.LENGTH_SHORT).show();

        } else if (results.isEmpty()) {
            Toast.makeText(this, "No results :(", Toast.LENGTH_SHORT).show();
            showAsListMenuItem.setVisible(false);
        } else {
            showAsListMenuItem.setVisible(results.size() > 1);
        }

        displayResultsOnMap(results);
        updateViewPager(results);
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

    private void updateViewPager(List<GeocodingResult> locations) {
        final boolean show = ((locations != null) && (!locations.isEmpty()));
        this.viewPager.setAdapter(new ViewPagerAdapter(locations));
        animateViewPager(show);
        if (show) {
            onPageSelected(0);
        }
        updateMapPadding();
    }

    private void animateViewPager(final boolean show) {
        final int visibility = (show ? View.VISIBLE : View.GONE);
        if (visibility == viewPagerContainer.getVisibility()) {
            return;
        }
        if (show) {
            this.viewPagerContainer.setVisibility(View.VISIBLE);
            this.viewPagerContainer.getViewTreeObserver()
                    .addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                        @Override
                        public boolean onPreDraw() {
                            viewPagerContainer.getViewTreeObserver().removeOnPreDrawListener(this);
                            viewPagerContainer.setTranslationY(viewPagerContainer.getHeight());
                            viewPagerContainer.setAlpha(0);
                            viewPagerContainer.animate().translationY(0).alpha(1)
                                    .setInterpolator(new AccelerateDecelerateInterpolator())
                                    .setDuration(200).start();
                            return true;
                        }
                    });
        } else {
            viewPagerContainer.setVisibility(View.GONE);
        }
    }

    /**
     * Remove previous results from the map and hide view pager
     */
    private void clearScreen() {
        removeResultMarkers();
        removeSelectedMarker();
        updateViewPager(null);
        if (getFragmentManager().getBackStackEntryCount() > 0) {
            getFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
    }

    private void updateMapPadding() {
        int l = 0;
        int t = 0;
        int r = 0;
        int b = 0;
        TypedValue val = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.actionBarSize, val, true);
        t = TypedValue.complexToDimensionPixelSize(val.data,
                getResources().getDisplayMetrics());

        if (viewPagerContainer.getVisibility() == View.VISIBLE) {
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
        this.viewPager.setAdapter(new ViewPagerAdapter(null));
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
            this.viewPager.setAdapter(new ViewPagerAdapter(null));
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

        public GeocodingResult getLocation(int position) {
            return locations.get(position);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (viewPager != null && viewPager.getAdapter() != null
                && viewPager.getAdapter().getCount() > 0) {
            List<GeocodingResult> content = ((ViewPagerAdapter) viewPager.getAdapter())
                    .getContent();
            if (content != null) {
                outState.putParcelableArray(CONTENT_EXTRA,
                        content.toArray(new GeocodingResult[content.size()]));
                outState.putInt(CURRENT_SELECTION_EXTRA, viewPager.getCurrentItem());
            }
        }
        super.onSaveInstanceState(outState);
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
}
