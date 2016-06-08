package com.bilko.findme;

import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
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
import com.bilko.findme.utils.Constants;
import com.bilko.findme.utils.FindMeUtils;

public class SignUpActivity extends BaseActivity implements ConnectionCallbacks,
        OnConnectionFailedListener {

    private static String TAG = SignUpActivity.class.getSimpleName();

    private TextInputEditText mFirstNameView;
    private TextInputEditText mLastNameView;
    private TextInputEditText mEmailView;
    private TextInputEditText mPasswordView;
    private View mSignUpForm;
    private View mSignUpProgress;

    private UserLocation mUserLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        mFirstNameView = (TextInputEditText) findViewById(R.id.first_name);
        mLastNameView = (TextInputEditText) findViewById(R.id.last_name);
        mEmailView = (TextInputEditText) findViewById(R.id.email);
        mPasswordView = (TextInputEditText) findViewById(R.id.password);
        mSignUpForm = findViewById(R.id.sign_up_form);
        mSignUpProgress = findViewById(R.id.sign_up_progress);

        Button mSignUpButton = (Button) findViewById(R.id.sign_up_button);
        if (mSignUpButton != null) {
            mSignUpButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    onSignUp();
                }
            });
        }

        //noinspection unchecked
        onBuildGoogleApiClient(this);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(SignUpActivity.this,
            android.Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(SignUpActivity.this,
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

    private void onSignUp() {
        final User user = new User(mFirstNameView.getText().toString().trim(),
            mLastNameView.getText().toString().trim(), mEmailView.getText().toString().trim(),
                mPasswordView.getText().toString().trim(), mUserLocation);

        if (onValidateRegistry(user)) {
            onCloseKeyboard();
            onShowProgress(mSignUpForm, mSignUpProgress, true);
            mFirebaseAuth
                .createUserWithEmailAndPassword(user.getEmail(), user.getPassword())
                .addOnSuccessListener(this, new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        onRegisterUser(user);
                        onShowProgress(mSignUpForm, mSignUpProgress, false);
                        onStartActivity(ListActivity.class);
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        onShowProgress(mSignUpForm, mSignUpProgress, false);
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

    private void onRegisterUser(final User user) {
        final String id = getUserId();
        if (!TextUtils.isEmpty(id)) {
            mDatabaseReference
                .child(Constants.USERS_DB_NODE)
                .child(id)
                .setValue(user)
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, e.getMessage());
                    }
                });
        } else {
            Log.e(TAG, getString(R.string.error_current_user));
        }
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
