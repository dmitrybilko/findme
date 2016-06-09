package com.bilko.findme;

import java.util.ArrayList;
import java.util.List;

import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import com.bilko.findme.adapters.UsersAdapter;
import com.bilko.findme.models.User;
import com.bilko.findme.models.UserLocation;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.widget.Toast.LENGTH_LONG;

import static com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY;

import static com.bilko.findme.utils.Constants.FASTEST_LOCATION_UPDATE_INTERVAL;
import static com.bilko.findme.utils.Constants.LOCATION_UPDATE_INTERVAL;
import static com.bilko.findme.utils.Constants.USERS_DB_NODE;

public class ListActivity extends BaseActivity implements ConnectionCallbacks,
        OnConnectionFailedListener, LocationListener {

    private static String TAG = ListActivity.class.getSimpleName();

    private View mListProgress;
    private ListView mUsersList;

    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        mListProgress = findViewById(R.id.list_progress);
        mUsersList = (ListView) findViewById(R.id.users_list);

        onShowProgress(mUsersList, mListProgress, true);

        final FloatingActionButton mMapsButton =
            (FloatingActionButton) findViewById(R.id.maps_button);
        if (mMapsButton != null) {
            mMapsButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View view) {
                    onStartActivity(MapsActivity.class);
                }
            });
        }

        onSyncUsersList();
        onCreateLocationRequest();

        //noinspection unchecked
        mGoogleApiClient = onBuildGoogleApiClient(this);
    }

    private void onSyncUsersList() {
        mDatabaseReference
            .child(USERS_DB_NODE)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    final List<User> users = new ArrayList<>();
                    for (DataSnapshot child: dataSnapshot.getChildren()) {
                        if (!child.getKey().equals(getUserId())) {
                            users.add(child.getValue(User.class));
                        }
                    }
                    if (mUsersList != null) {
                        mUsersList.setAdapter(new UsersAdapter(ListActivity.this, users));
                        onShowProgress(mUsersList, mListProgress, false);
                    }
                }
                @Override
                public void onCancelled(DatabaseError databaseError) {
                    if (mUsersList != null) {
                        onShowProgress(mUsersList, mListProgress, false);
                        Toast
                            .makeText(ListActivity.this, databaseError.getMessage(), LENGTH_LONG)
                            .show();
                    }
                }
            });
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        onStartLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        Log.e(TAG, result.getErrorMessage());
    }

    private void onCreateLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(LOCATION_UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_LOCATION_UPDATE_INTERVAL);
        mLocationRequest.setPriority(PRIORITY_HIGH_ACCURACY);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    private void onStartLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION) == PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) == PERMISSION_GRANTED) {
            LocationServices
                .FusedLocationApi
                .requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        } else {
            Toast
                .makeText(this, getString(R.string.error_check_permissions), LENGTH_LONG)
                .show();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            onSyncLocation(TAG, new UserLocation(location.getLongitude(), location.getLatitude()));
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        mFirebaseAuth.signOut();
        onStartActivity(SignInActivity.class);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mGoogleApiClient.isConnected()) {
            onStopLocationUpdates();
        }
    }

    private void onStopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }
}
