/*
 * Copyright 2014 Mapzen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.lost;

import com.mapzen.android.lost.LocationClient;
import com.mapzen.android.lost.LocationListener;
import com.mapzen.android.lost.LocationRequest;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.ListFragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Date;

/**
 * LOST Activity
 */
public class LostActivity extends Activity {
    LostFragment fragment;
    LocationClient client;
    SharedPreferences sharedPreferences;

    LocationClient.ConnectionCallbacks callbacks = new LocationClient.ConnectionCallbacks() {
        @Override
        public void onConnected(Bundle connectionHint) {
            Toast.makeText(getApplication(), "Location client connected",
                    Toast.LENGTH_SHORT).show();

            if (fragment.lastKnownLocation == null) {
                fragment.setLastKnownLocation(client.getLastLocation());
            }

            LocationRequest locationRequest = LocationRequest.create();
            locationRequest.setInterval(5000);
            locationRequest.setSmallestDisplacement(0);
            client.requestLocationUpdates(locationRequest, listener);
        }

        @Override
        public void onDisconnected() {
            Toast.makeText(getApplication(), "Location client disconnected",
                    Toast.LENGTH_SHORT).show();
        }
    };

    LocationListener listener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            fragment.updateLocation(location);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final FragmentManager fragmentManager = getFragmentManager();
        fragment = (LostFragment) fragmentManager.findFragmentByTag(LostFragment.TAG);
        if (fragment == null) {
            fragment = new LostFragment();
            fragmentManager.beginTransaction().add(android.R.id.content, fragment,
                    LostFragment.TAG).commit();
            fragment.setListAdapter(new LostAdapter(this));
            client = new LocationClient(this, callbacks);
            fragment.client = client;
        } else {
            client = fragment.client;
        }

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplication());
    }

    @Override
    protected void onResume() {
        super.onResume();
        client.setMockMode(isMockModePrefEnabled());
        client.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        client.disconnect();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        startActivity(new Intent(this, SettingsActivity.class));
        return true;
    }

    private boolean isMockModePrefEnabled() {
        return sharedPreferences.getBoolean(getString(R.string.mock_mode_key), false);
    }

    public static void populateLocationView(Location location, View view) {
        final TextView provider = (TextView) view.findViewById(R.id.provider);
        final TextView coordinates = (TextView) view.findViewById(R.id.coordinates);
        final TextView accuracy = (TextView) view.findViewById(R.id.accuracy);
        final TextView time = (TextView) view.findViewById(R.id.time);

        provider.setText(location.getProvider() + " provider");
        coordinates.setText(location.getLatitude() + ", " + location.getLongitude());
        accuracy.setText("within " + Math.round(location.getAccuracy()) + " meters");
        time.setText(new Date(location.getTime()).toString());
    }

    /**
     * LOST Fragment
     */
    private static class LostFragment extends ListFragment {
        public static final String TAG = LostFragment.class.getSimpleName();

        private LocationClient client;
        private Location lastKnownLocation;
        private View headerView;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            final View view = inflater.inflate(R.layout.fragment_lost, container, false);
            final ListView listView = (ListView) view.findViewById(android.R.id.list);
            headerView = inflater.inflate(R.layout.list_header, null);
            listView.addHeaderView(headerView);
            listView.setHeaderDividersEnabled(false);
            if (lastKnownLocation != null) {
                populateLocationView(lastKnownLocation, headerView);
            }
            return view;
        }

        public void setLastKnownLocation(Location location) {
            lastKnownLocation = location;
            populateLocationView(location, headerView);
        }

        public void updateLocation(Location location) {
            final LostAdapter adapter = (LostAdapter) getListAdapter();
            adapter.addLocation(location);
            adapter.notifyDataSetChanged();
        }
    }

    /**
     * LOST Adapter
     */
    private static class LostAdapter extends BaseAdapter {
        private final ArrayList<Location> locations = new ArrayList<Location>();
        private final Context context;

        public LostAdapter(Context context) {
            this.context = context;
        }

        public void addLocation(Location location) {
            locations.add(location);
        }

        @Override
        public int getCount() {
            return locations.size();
        }

        @Override
        public Object getItem(int i) {
            return locations.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (view == null) {
                view = View.inflate(context, R.layout.list_item, null);
            }

            populateLocationView(locations.get(i), view);
            return view;
        }
    }
}
