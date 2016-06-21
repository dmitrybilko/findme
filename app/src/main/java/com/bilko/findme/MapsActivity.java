package com.bilko.findme;

import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import com.bilko.findme.models.User;
import com.bilko.findme.models.UserLocation;

import static com.bilko.findme.utils.Constants.USERS_DB_NODE;

public class MapsActivity extends BaseActivity implements ValueEventListener {

    private static String TAG = MapsActivity.class.getSimpleName();

    @Override
    protected void onCreate(final Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_maps);

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            onStartActivity(MainActivity.class);
        }

        final MapFragment mMapFragment =
            (MapFragment) getFragmentManager().findFragmentById(R.id.maps_container_fragment_map);
        if (mMapFragment != null) {
            mMapFragment.getMapAsync(this);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        onSyncMarkers();
    }

    private void onSyncMarkers() {
        getDatabaseReference()
            .child(USERS_DB_NODE)
            .addListenerForSingleValueEvent(this);
    }

    @Override
    public void onDataChange(final DataSnapshot mDataSnapshot) {
        for (DataSnapshot child: mDataSnapshot.getChildren()) {
            if (!child.getKey().equals(getUserId())) {
                final User user = child.getValue(User.class);
                final UserLocation mUserLocation = user.getUserLocation();
                if (mUserLocation != null) {
                    setMarker(TAG, new LatLng(mUserLocation.getLatitude(), mUserLocation.getLongitude()),
                        user.getFullName(), BitmapDescriptorFactory.defaultMarker());
                } else {
                    Log.e(TAG, getString(R.string.error_user_location) + ". USER ID: "
                        + child.getKey());
                }
            }
        }
    }

    @Override
    public void onCancelled(final DatabaseError mDatabaseError) {
        Log.e(TAG, mDatabaseError.getMessage());
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        onStartActivity(MainActivity.class);
    }

    @Override
    protected void onStop() {
        super.onStop();
        onStopSyncMarkers();
        if (getGoogleMap() != null) {
            getGoogleMap().clear();
        }
    }

    private void onStopSyncMarkers() {
        getDatabaseReference().removeEventListener(this);
    }
}
