package com.bilko.findme;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class BaseActivity extends Activity implements OnMapReadyCallback  {

    private FirebaseAuth mFirebaseAuth;
    private DatabaseReference mDatabaseReference;
    private GoogleMap mGoogleMap;

    protected FirebaseAuth getFirebaseAuth() {
        if (mFirebaseAuth == null) {
            mFirebaseAuth = FirebaseAuth.getInstance();
        }
        return mFirebaseAuth;
    }

    protected DatabaseReference getDatabaseReference() {
        if (mDatabaseReference == null) {
            mDatabaseReference = FirebaseDatabase.getInstance().getReference();
        }
        return mDatabaseReference;
    }

    protected GoogleMap getGoogleMap() {
        return mGoogleMap;
    }

    protected boolean onCheckTextViewError(final TextView view, final String error) {
        if (TextUtils.isEmpty(error)) {
            return true;
        }
        view.setError(error);
        view.requestFocus();
        return false;
    }

    protected void onCloseKeyboard() {
        final View view = this.getCurrentFocus();
        if (view != null) {
            final InputMethodManager manager =
                (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            manager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    protected void onStartActivity(final Class activity) {
        startActivity(new Intent(this, activity));
        finish();
    }

    protected String getUserId() {
        final FirebaseUser mCurrentUser = getFirebaseAuth().getCurrentUser();
        if (mCurrentUser != null) {
            return mCurrentUser.getUid();
        }
        return "";
    }

    @Override
    public void onMapReady(final GoogleMap mGoogleMap) {
        this.mGoogleMap = mGoogleMap;
    }

    protected void setMarker(final String TAG, final LatLng mLatLng, final String title,
        final BitmapDescriptor bd) {
        if (mGoogleMap != null) {
            mGoogleMap
                .addMarker(new MarkerOptions()
                    .position(mLatLng)
                    .title(title)
                    .icon(bd));

        } else {
            Log.e(TAG, getString(R.string.error_google_map));
        }
    }
}
