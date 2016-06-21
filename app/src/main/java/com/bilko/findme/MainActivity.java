package com.bilko.findme;

import java.util.ArrayList;
import java.util.List;

import android.app.ListFragment;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnFailureListener;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import com.bilko.findme.models.User;
import com.bilko.findme.models.UserLocation;

import static android.widget.Toast.LENGTH_LONG;

import static com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY;

import static com.bilko.findme.utils.Constants.USER_LOCATION_DB_NODE;
import static com.bilko.findme.utils.Constants.FASTEST_LOCATION_UPDATE_INTERVAL;
import static com.bilko.findme.utils.Constants.LOCATION_UPDATE_INTERVAL;
import static com.bilko.findme.utils.Constants.USERS_DB_NODE;

public class MainActivity extends BaseActivity implements ConnectionCallbacks,
        OnConnectionFailedListener, LocationListener, OnClickListener, ValueEventListener {

    private static String TAG = MainActivity.class.getSimpleName();

    private ListFragment mListFragment;
    private MapFragment mMapFragment;

    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mListFragment =
            (ListFragment) getFragmentManager().findFragmentById(R.id.main_container_fragment_list);
        mMapFragment =
            (MapFragment) getFragmentManager().findFragmentById(R.id.main_container_fragment_map);
        if (mMapFragment != null) {
            mMapFragment.getMapAsync(this);
        }

        final View mMapsButton = findViewById(R.id.maps_button);
        if (mMapsButton != null) {
            mMapsButton.setOnClickListener(this);
        }

        onCreateLocationRequest();
        onBuildGoogleApiClient();
    }

    @Override
    public void onClick(final View view) {
        switch (view.getId()) {
            case R.id.maps_button:
                onStartActivity(MapsActivity.class);
        }
    }

    private void onCreateLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(LOCATION_UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_LOCATION_UPDATE_INTERVAL);
        mLocationRequest.setPriority(PRIORITY_HIGH_ACCURACY);
    }

    private void onBuildGoogleApiClient() {
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        onSyncUsersList();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onConnected(@Nullable final Bundle bundle) {
        onStartLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(final int i) {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onConnectionFailed(@NonNull final ConnectionResult result) {
        Log.e(TAG, result.getErrorMessage());
    }

    private void onStartLocationUpdates() {
        try {
            LocationServices
                .FusedLocationApi
                .requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        } catch (final SecurityException e) {
            Toast
                .makeText(this, getString(R.string.error_check_permissions), LENGTH_LONG)
                .show();
        }
    }

    @Override
    public void onLocationChanged(final Location location) {
        if (location != null) {
            onSyncLocation(new UserLocation(location.getLongitude(), location.getLatitude()));
        }
    }

    private void onSyncLocation(final UserLocation mUserLocation) {
        final String id = getUserId();
        if (TextUtils.isEmpty(id)) {
            Log.e(TAG, getString(R.string.error_current_user));
            return;
        }

        getDatabaseReference()
            .child(USERS_DB_NODE)
            .child(id)
            .child(USER_LOCATION_DB_NODE)
            .setValue(mUserLocation)
            .addOnFailureListener(this, new OnFailureListener() {
                @Override
                public void onFailure(@NonNull final Exception e) {
                    Log.e(TAG, e.getMessage());
                }
            });
    }

    private void onSyncUsersList() {
        if (getFirebaseAuth().getCurrentUser() != null) {
            getDatabaseReference()
                .child(USERS_DB_NODE)
                .addListenerForSingleValueEvent(this);
        } else {
            onStartActivity(SignInActivity.class);
        }
    }

    @Override
    public void onDataChange(final DataSnapshot mDataSnapshot) {
        final List<String> users = new ArrayList<>();
        for (DataSnapshot child: mDataSnapshot.getChildren()) {
            if (!child.getKey().equals(getUserId())) {
                final User user = child.getValue(User.class);
                final String mUserFullName = user.getFullName();

                if (mMapFragment != null) {
                    final UserLocation mUserLocation = user.getUserLocation();
                    if (mUserLocation != null) {
                        final LatLng mLatLng =
                            new LatLng(mUserLocation.getLatitude(), mUserLocation.getLongitude());
                        setMarker(TAG, mLatLng, mUserFullName, BitmapDescriptorFactory.defaultMarker());
                    } else {
                        Log.e(TAG, getString(R.string.error_user_location) + ". USER ID: "
                            + child.getKey());
                    }
                }

                users.add(mUserFullName);
            }
        }
        if (mListFragment != null) {
            mListFragment.setListAdapter(
                new ArrayAdapter<>(this, R.layout.item_user, R.id.user_full_name, users));
        }
    }

    @Override
    public void onCancelled(final DatabaseError mDatabaseError) {
        Log.e(TAG, mDatabaseError.getMessage());
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        getFirebaseAuth().signOut();
        onStartActivity(SignInActivity.class);
    }

    @Override
    protected void onStop() {
        super.onStop();
        onStopSyncUsersList();
        if (mGoogleApiClient.isConnected()) {
            onStopLocationUpdates();
            mGoogleApiClient.disconnect();
        }
        if (getGoogleMap() != null) {
            getGoogleMap().clear();
        }
    }

    private void onStopSyncUsersList() {
        getDatabaseReference().removeEventListener(this);
    }

    private void onStopLocationUpdates() {
        LocationServices
            .FusedLocationApi
            .removeLocationUpdates(mGoogleApiClient, this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMapFragment = null;
    }
}
