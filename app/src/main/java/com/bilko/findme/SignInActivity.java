package com.bilko.findme;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.bilko.findme.models.User;
import com.bilko.findme.utils.FindMeUtils;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;

public class SignInActivity extends BaseActivity implements OnClickListener {

    private TextInputEditText mEmailView;
    private TextInputEditText mPasswordView;
    private View mSignInProgress;
    private View mSignInForm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        mEmailView = (TextInputEditText) findViewById(R.id.email);
        mPasswordView = (TextInputEditText) findViewById(R.id.password);
        mSignInForm = findViewById(R.id.sign_in_form);
        mSignInProgress = findViewById(R.id.sign_in_progress);

        Button mSignInButton = (Button) findViewById(R.id.sign_in_button);
        if (mSignInButton != null) {
            mSignInButton.setOnClickListener(this);
        }

        TextView mSignUpButton = (TextView) findViewById(R.id.sign_up_view);
        if (mSignUpButton != null) {
            mSignUpButton.setOnClickListener(this);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mFirebaseAuth.getCurrentUser() != null) {
            onStartActivity(ListActivity.class);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sign_in_button:
                onSignIn();
                return;
            case R.id.sign_up_view:
                onStartActivity(SignUpActivity.class);
        }
    }

    private void onSignIn() {
        final User mUser = new User();
        mUser.setEmail(mEmailView.getText().toString().trim());
        mUser.setPassword(mPasswordView.getText().toString().trim());

        if (onValidateCredentials(mUser)) {
            onCloseKeyboard();
            onShowProgress(mSignInForm, mSignInProgress, true);
            mFirebaseAuth
                .signInWithEmailAndPassword(mUser.getEmail(), mUser.getPassword())
                .addOnSuccessListener(this, new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        onShowProgress(mSignInForm, mSignInProgress, false);
                        onStartActivity(ListActivity.class);
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        onShowProgress(mSignInForm, mSignInProgress, false);
                        Snackbar
                            .make(mSignInForm, e.getMessage(), Snackbar.LENGTH_LONG)
                            .show();
                    }
                });
        }
    }

    private boolean onValidateCredentials(final User mUser) {
        mEmailView.setError(null);
        mPasswordView.setError(null);

        return (onCheckTextViewError(mEmailView, FindMeUtils.isEmailValid(this, mUser.getEmail()))
            && onCheckTextViewError(mPasswordView, FindMeUtils.isPasswordValid(this, mUser.getPassword())));
    }
}
