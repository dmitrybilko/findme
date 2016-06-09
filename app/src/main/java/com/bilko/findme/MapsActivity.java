package com.bilko.findme;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import com.bilko.findme.models.User;
import com.bilko.findme.models.UserLocation;

import static android.widget.Toast.LENGTH_LONG;

import static com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_AZURE;

import static com.bilko.findme.utils.Constants.USERS_DB_NODE;

public class MapsActivity extends BaseActivity implements OnMapReadyCallback {

    private static String TAG = MapsActivity.class.getSimpleName();

    private GoogleMap mGoogleMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        SupportMapFragment mapFragment =
            (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(final GoogleMap mGoogleMap) {
        this.mGoogleMap = mGoogleMap;
    }

    @Override
    protected void onResume() {
        super.onResume();
        onSyncMarkers();
    }

    private void onSyncMarkers() {
        mDatabaseReference
            .child(USERS_DB_NODE)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    for (DataSnapshot child: dataSnapshot.getChildren()) {
                        final User user = child.getValue(User.class);
                        final UserLocation mUserLocation = user.getUserLocation();
                        if (mUserLocation != null) {
                            final LatLng mLatLng = new LatLng(mUserLocation.getLatitude(),
                                mUserLocation.getLongitude());
                            if (child.getKey().equals(getUserId())) {
                                setMarker(mLatLng, getString(R.string.look_great),
                                    BitmapDescriptorFactory.defaultMarker(HUE_AZURE));
                                mGoogleMap.animateCamera(CameraUpdateFactory.newLatLng(mLatLng));
                            } else {
                                setMarker(mLatLng, user.getFullName(),
                                    BitmapDescriptorFactory.defaultMarker());
                            }
                        } else {
                            Log.e(TAG, getString(R.string.error_user_location));
                        }
                    }
                }
                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Toast
                        .makeText(MapsActivity.this, databaseError.getMessage(), LENGTH_LONG)
                        .show();
                }
            });
    }

    private void setMarker(final LatLng mLatLng, final String title, final BitmapDescriptor bd) {
        mGoogleMap
            .addMarker(new MarkerOptions()
                .position(mLatLng)
                .title(title)
                .icon(bd));
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        onStartActivity(ListActivity.class);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mGoogleMap != null) {
            mGoogleMap.clear();
        }
    }
}
