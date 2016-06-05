package com.bilko.findme.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bilko.findme.R;
import com.bilko.findme.models.User;

import java.util.ArrayList;
import java.util.List;

public class UsersAdapter extends AbstractAdapter {

    final List<User> users = new ArrayList<>();

    public UsersAdapter(final Context context, final List<User> users) {
        super(context);
        this.users.addAll(users);
    }

    @Override
    public int getCount() {
        return users.size();
    }

    @Override
    public User getItem(int position) {
        return users.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View mUserView, ViewGroup parent) {
        final UserHolder mUserHolder;
        final User user = getItem(position);

        if (mUserView == null) {
            mUserView = mLayoutInflater.inflate(R.layout.item_user, parent, false);
            mUserHolder = new UserHolder(mUserView);
            mUserView.setTag(mUserHolder);
        } else {
            mUserHolder = (UserHolder) mUserView.getTag();
        }

        if (user != null && !user.getFullName().isEmpty()) {
            mUserHolder.setFullName(user.getFullName());
        }

        return mUserView;
    }

    private class UserHolder {
        private final TextView mFullNameView;

        private UserHolder(View mConvertView) {
            mFullNameView = (TextView) mConvertView.findViewById(R.id.user_full_name);
        }

        public void setFullName(String mFullName) {
            this.mFullNameView.setText(mFullName);
        }
    }
}
