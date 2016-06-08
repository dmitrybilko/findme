package com.bilko.findme;

import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import com.google.firebase.auth.AuthResult;

import com.bilko.findme.models.User;
import com.bilko.findme.models.UserLocation;
import com.bilko.findme.utils.FindMeUtils;

public class SignInActivity extends BaseActivity implements ConnectionCallbacks,
        OnConnectionFailedListener {

    private static String TAG = SignInActivity.class.getSimpleName();

    private TextInputEditText mEmailView;
    private TextInputEditText mPasswordView;
    private View mSignInProgress;
    private View mSignInForm;

    private UserLocation mUserLocation;

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

        //noinspection unchecked
        onBuildGoogleApiClient(this);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(SignInActivity.this,
            android.Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(SignInActivity.this,
                        android.Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {
            final Location mCurrentLocation =
                LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (mCurrentLocation != null) {
                mUserLocation =
                    new UserLocation(mCurrentLocation.getLongitude(), mCurrentLocation.getLatitude());
            }
        } else {
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
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        Log.e(TAG, result.getErrorMessage());
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
                        onSyncLocation(TAG, mUserLocation);
                        onShowProgress(mSignInForm, mSignInProgress, false);
                        onStartActivity(ListActivity.class);
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull final Exception e) {
                        onShowProgress(mSignInForm, mSignInProgress, false);
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
            && onCheckTextViewError(mPasswordView, FindMeUtils.isPasswordValid(this,
                user.getPassword())));
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }
}
