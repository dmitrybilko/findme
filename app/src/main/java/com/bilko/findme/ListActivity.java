package com.bilko.findme;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import com.bilko.findme.adapters.UsersAdapter;
import com.bilko.findme.models.User;
import com.bilko.findme.utils.Constants;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ListActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        final View mListProgress = findViewById(R.id.list_progress);
        final ListView mUsersList = (ListView) findViewById(R.id.users_list);

        onShowProgress(mUsersList, mListProgress, true);

        final FloatingActionButton mMapsButton =
            (FloatingActionButton) findViewById(R.id.maps_button);
        if (mMapsButton != null) {
            mMapsButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View view) {
                    onStartActivity(MapsActivity.class);
                }
            });
        }

        mDatabaseReference
            .child(Constants.USERS_DB_NODE)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    final List<User> users = new ArrayList<>();
                    for (DataSnapshot child: dataSnapshot.getChildren()) {
                        users.add(child.getValue(User.class));
                    }
                    if (mUsersList != null) {
                        mUsersList.setAdapter(new UsersAdapter(ListActivity.this, users));
                        onShowProgress(mUsersList, mListProgress, false);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    if (mUsersList != null) {
                        onShowProgress(mUsersList, mListProgress, false);
                        Snackbar
                            .make(mUsersList, databaseError.getMessage(), Snackbar.LENGTH_LONG)
                            .show();
                    }
                }
            });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        mFirebaseAuth.signOut();
        onStartActivity(SignInActivity.class);
    }
}
