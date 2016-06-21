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
import com.bilko.findme.utils.Constants;
import com.bilko.findme.utils.FindMeUtils;

public class SignUpActivity extends BaseActivity implements ConnectionCallbacks,
        OnConnectionFailedListener, OnClickListener {

    private static String TAG = SignUpActivity.class.getSimpleName();

    private TextInputEditText mFirstNameView;
    private TextInputEditText mLastNameView;
    private TextInputEditText mEmailView;
    private TextInputEditText mPasswordView;
    private View mSignUpProgress;
    private View mSignUpForm;

    private GoogleApiClient mGoogleApiClient;
    private UserLocation mUserLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        mFirstNameView = (TextInputEditText) findViewById(R.id.first_name);
        mLastNameView = (TextInputEditText) findViewById(R.id.last_name);
        mEmailView = (TextInputEditText) findViewById(R.id.email);
        mPasswordView = (TextInputEditText) findViewById(R.id.password);
        mSignUpProgress = findViewById(R.id.sign_up_progress);
        mSignUpForm = findViewById(R.id.sign_up_form);

        final View mSignUpButton = findViewById(R.id.sign_up_button);
        if (mSignUpButton != null) {
            mSignUpButton.setOnClickListener(this);
        }

        onBuildGoogleApiClient();
    }

    @Override
    public void onClick(final View view) {
        switch (view.getId()) {
            case R.id.sign_up_button:
                onSignUp();
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

    private void onSignUp() {
        final User user = new User(
            mFirstNameView.getText().toString().trim(),
            mLastNameView.getText().toString().trim(),
            mEmailView.getText().toString().trim(),
            mPasswordView.getText().toString().trim(),
            mUserLocation
        );

        if (onValidateRegistry(user)) {
            onCloseKeyboard();
            onShowProgress(true);
            getFirebaseAuth()
                .createUserWithEmailAndPassword(user.getEmail(), user.getPassword())
                .addOnSuccessListener(this, new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(final AuthResult authResult) {
                        onRegisterUser(user);
                        onStartActivity(MainActivity.class);
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull final Exception e) {
                        onShowProgress(false);
                        Toast
                            .makeText(SignUpActivity.this, e.getMessage(), Toast.LENGTH_LONG)
                            .show();
                    }
                });
        }
    }

    private boolean onValidateRegistry(final User mUser) {
        mFirstNameView.setError(null);
        mLastNameView.setError(null);
        mEmailView.setError(null);
        mPasswordView.setError(null);

        return (onCheckTextViewError(mFirstNameView, FindMeUtils.isTextValid(this, mUser.getFirstName()))
            && onCheckTextViewError(mLastNameView, FindMeUtils.isTextValid(this, mUser.getLastName()))
            && onCheckTextViewError(mEmailView, FindMeUtils.isEmailValid(this, mUser.getEmail()))
            && onCheckTextViewError(mPasswordView, FindMeUtils.isPasswordValid(this, mUser.getPassword())));
    }

    private void onShowProgress(final boolean show) {
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);
        mSignUpForm.setVisibility(show ? View.GONE : View.VISIBLE);
        mSignUpProgress.setVisibility(show ? View.VISIBLE : View.GONE);
        mSignUpProgress
            .animate()
            .setDuration(shortAnimTime)
            .alpha(show ? 1 : 0)
            .setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mSignUpProgress.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
    }

    private void onRegisterUser(final User user) {
        final String id = getUserId();
        if (TextUtils.isEmpty(id)) {
            Log.e(TAG, getString(R.string.error_current_user));
        }

        getDatabaseReference()
            .child(Constants.USERS_DB_NODE)
            .child(id)
            .setValue(user)
            .addOnFailureListener(this, new OnFailureListener() {
                @Override
                public void onFailure(@NonNull final Exception e) {
                    Log.e(TAG, e.getMessage());
                }
            });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        onStartActivity(SignInActivity.class);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }
}
