package com.bilko.findme;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import com.google.firebase.auth.AuthResult;

import com.bilko.findme.models.User;
import com.bilko.findme.models.UserLocation;
import com.bilko.findme.utils.FindMeUtils;

import static com.bilko.findme.utils.Constants.USERS_DB_NODE;
import static com.bilko.findme.utils.Constants.USER_LOCATION_DB_NODE;

public class SignInActivity extends BaseActivity implements ConnectionCallbacks,
        OnConnectionFailedListener, OnClickListener {

    private static String TAG = SignInActivity.class.getSimpleName();

    private TextInputEditText mEmailView;
    private TextInputEditText mPasswordView;
    private View mSignInProgress;
    private View mSignInForm;

    private GoogleApiClient mGoogleApiClient;
    private UserLocation mUserLocation;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        mEmailView = (TextInputEditText) findViewById(R.id.email);
        mPasswordView = (TextInputEditText) findViewById(R.id.password);
        mSignInProgress = findViewById(R.id.sign_in_progress);
        mSignInForm = findViewById(R.id.sign_in_form);

        final View mSignInButton = findViewById(R.id.sign_in_button);
        if (mSignInButton != null) {
            mSignInButton.setOnClickListener(this);
        }

        final View mSignUpView = findViewById(R.id.sign_up_view);
        if (mSignUpView != null) {
            mSignUpView.setOnClickListener(this);
        }

        onBuildGoogleApiClient();
    }

    @Override
    public void onClick(final View view) {
        switch (view.getId()) {
            case R.id.sign_in_button:
                onSignIn();
                return;
            case R.id.sign_up_view:
                onStartActivity(SignUpActivity.class);
        }
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
        if (getFirebaseAuth().getCurrentUser() != null) {
            onStartActivity(MainActivity.class);
        }
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onConnected(@Nullable final Bundle bundle) {
        try {
            final Location mCurrentLocation =
                LocationServices
                    .FusedLocationApi
                    .getLastLocation(mGoogleApiClient);
            if (mCurrentLocation != null) {
                mUserLocation =
                    new UserLocation(mCurrentLocation.getLongitude(), mCurrentLocation.getLatitude());
            }
        } catch (final SecurityException e) {
            Toast
                .makeText(this, getString(R.string.error_check_permissions), Toast.LENGTH_LONG)
                .show();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onConnectionFailed(@NonNull final ConnectionResult result) {
        Log.e(TAG, result.getErrorMessage());
    }

    private void onSignIn() {
        final User user = new User();
        user.setEmail(mEmailView.getText().toString().trim());
        user.setPassword(mPasswordView.getText().toString().trim());

        if (onValidateCredentials(user)) {
            onCloseKeyboard();
            onShowProgress(true);
            getFirebaseAuth()
                .signInWithEmailAndPassword(user.getEmail(), user.getPassword())
                .addOnSuccessListener(this, new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(final AuthResult mAuthResult) {
                        onSyncLocation();
                        onStartActivity(MainActivity.class);
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull final Exception e) {
                        onShowProgress(false);
                        Toast
                            .makeText(SignInActivity.this, e.getMessage(), Toast.LENGTH_LONG)
                            .show();
                    }
                });
        }
    }

    private boolean onValidateCredentials(final User user) {
        mEmailView.setError(null);
        mPasswordView.setError(null);

        return (onCheckTextViewError(mEmailView, FindMeUtils.isEmailValid(this, user.getEmail()))
            && onCheckTextViewError(mPasswordView, FindMeUtils.isPasswordValid(this, user.getPassword())));
    }

    private void onShowProgress(final boolean show) {
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);
        mSignInForm.setVisibility(show ? View.GONE : View.VISIBLE);
        mSignInProgress.setVisibility(show ? View.VISIBLE : View.GONE);
        mSignInProgress
            .animate()
            .setDuration(shortAnimTime)
            .alpha(show ? 1 : 0)
            .setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mSignInProgress.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
    }

    private void onSyncLocation() {
        final String id = getUserId();
        if (TextUtils.isEmpty(id)) {
            Log.e(TAG, getString(R.string.error_current_user));
            return;
        }
        if (mUserLocation == null) {
            Log.e(TAG, getString(R.string.error_user_location) + ". USER ID: " + id);
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

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        mUserLocation = null;
    }
}
