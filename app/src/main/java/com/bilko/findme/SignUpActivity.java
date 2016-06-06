package com.bilko.findme;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.bilko.findme.models.User;
import com.bilko.findme.utils.Constants;
import com.bilko.findme.utils.FindMeUtils;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;

public class SignUpActivity extends BaseActivity implements OnClickListener {

    private static String TAG = SignUpActivity.class.getSimpleName();

    private TextInputEditText mFirstNameView;
    private TextInputEditText mLastNameView;
    private TextInputEditText mEmailView;
    private TextInputEditText mPasswordView;
    private View mSignUpForm;
    private View mSignUpProgress;

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
            mSignUpButton.setOnClickListener(this);
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.sign_up_button) {
            onSignUp();
        }
    }

    private void onSignUp() {
        final User user = new User(
            mFirstNameView.getText().toString().trim(), mLastNameView.getText().toString().trim(),
            mEmailView.getText().toString().trim(), mPasswordView.getText().toString().trim()
        );

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
                        Snackbar
                            .make(mSignUpForm, e.getMessage(), Snackbar.LENGTH_LONG)
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
                        Snackbar
                            .make(mSignUpForm, getString(R.string.error_db_transaction),
                                Snackbar.LENGTH_LONG)
                            .show();
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
}
