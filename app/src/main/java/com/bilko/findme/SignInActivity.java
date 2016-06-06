package com.bilko.findme;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import com.google.firebase.auth.AuthResult;

import com.bilko.findme.models.User;
import com.bilko.findme.utils.Constants;
import com.bilko.findme.utils.FindMeUtils;

public class SignInActivity extends BaseActivity implements LocationListener {

    private static String TAG = SignInActivity.class.getSimpleName();

    private TextInputEditText mEmailView;
    private TextInputEditText mPasswordView;
    private View mSignInProgress;
    private View mSignInForm;

    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private Location mCurrentLocation;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        mEmailView = (TextInputEditText) findViewById(R.id.email);
        mPasswordView = (TextInputEditText) findViewById(R.id.password);
        mSignInForm = findViewById(R.id.sign_in_form);
        mSignInProgress = findViewById(R.id.sign_in_progress);

        Button mSignInButton = (Button) findViewById(R.id.sign_in_button);
        if (mSignInButton != null) {
            mSignInButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    onSignIn();
                }
            });
        }

        TextView mSignUpButton = (TextView) findViewById(R.id.sign_up_view);
        if (mSignUpButton != null) {
            mSignUpButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    onStartActivity(SignUpActivity.class);
                }
            });
        }

        onBuildGoogleApiClient();
    }

    private synchronized void onBuildGoogleApiClient() {
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new ConnectionCallbacks() {
                    @Override
                    public void onConnected(@Nullable Bundle bundle) {
                        if (mCurrentLocation == null) {
                            if (ActivityCompat.checkSelfPermission(SignInActivity.this,
                                android.Manifest.permission.ACCESS_COARSE_LOCATION)
                                    == PackageManager.PERMISSION_GRANTED
                                        && ActivityCompat.checkSelfPermission(SignInActivity.this,
                                            android.Manifest.permission.ACCESS_FINE_LOCATION)
                                                == PackageManager.PERMISSION_GRANTED) {
                                mCurrentLocation = LocationServices
                                    .FusedLocationApi
                                    .getLastLocation(mGoogleApiClient);
                                if (mCurrentLocation != null) {
                                    final double latitude = mCurrentLocation.getLatitude();
                                    final double longitude = mCurrentLocation.getLongitude();
                                    Snackbar
                                        .make(mSignInForm, latitude + ", " + longitude,
                                            Snackbar.LENGTH_LONG)
                                        .show();
                                }
                            } else {
                                Log.e(TAG, getString(R.string.error_check_permissions));
                            }
                        }
                        onStartLocationUpdates();
                    }
                    @Override
                    public void onConnectionSuspended(int i) {
                        if (mGoogleApiClient != null) {
                            mGoogleApiClient.connect();
                        }
                    }
                })
                .addOnConnectionFailedListener(new OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(@NonNull final ConnectionResult result) {
                        Log.e(TAG, getString(R.string.error_connection) + ": "
                            + result.getErrorMessage());
                    }
                })
                .addApi(LocationServices.API)
                .build();
        }
        onCreateLocationRequest();
    }

    private void onStartLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationServices
                .FusedLocationApi
                .requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        } else {
            Log.e(TAG, getString(R.string.error_check_permissions));
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            final double latitude = mCurrentLocation.getLatitude();
            final double longitude = mCurrentLocation.getLongitude();
            Snackbar
                .make(mSignInForm, latitude + ", " + longitude, Snackbar.LENGTH_LONG)
                .show();
        }
    }

    private void onCreateLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(Constants.LOCATION_UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(Constants.FASTEST_LOCATION_UPDATE_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mFirebaseAuth.getCurrentUser() != null) {
            onStartActivity(ListActivity.class);
        }
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    private void onSignIn() {
        final User user = new User();
        user.setEmail(mEmailView.getText().toString().trim());
        user.setPassword(mPasswordView.getText().toString().trim());

        if (onValidateCredentials(user)) {
            onCloseKeyboard();
            onShowProgress(mSignInForm, mSignInProgress, true);
            mFirebaseAuth
                .signInWithEmailAndPassword(user.getEmail(), user.getPassword())
                .addOnSuccessListener(this, new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(final AuthResult mAuthResult) {
                        onShowProgress(mSignInForm, mSignInProgress, false);
                        onStartActivity(ListActivity.class);
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull final Exception e) {
                        onShowProgress(mSignInForm, mSignInProgress, false);
                        Snackbar
                            .make(mSignInForm, e.getMessage(), Snackbar.LENGTH_LONG)
                            .show();
                    }
                });
        }
    }

    private boolean onValidateCredentials(final User user) {
        mEmailView.setError(null);
        mPasswordView.setError(null);

        return (onCheckTextViewError(mEmailView, FindMeUtils.isEmailValid(this, user.getEmail()))
            && onCheckTextViewError(mPasswordView, FindMeUtils.isPasswordValid(this,
                user.getPassword())));
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
