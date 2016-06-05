package com.bilko.findme.utils;

import android.content.Context;
import android.text.TextUtils;
import android.util.Patterns;

import com.bilko.findme.R;

public class FindMeUtils {

    public static String isEmailValid(Context context, String email) {
        String error = "";
        if (TextUtils.isEmpty(email)) {
            error = context.getString(R.string.error_field_required);
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            error = context.getString(R.string.error_invalid_email);
        }
        return error;
    }

    public static String isPasswordValid(Context context, String password) {
        String error = "";
        if (TextUtils.isEmpty(password)) {
            error = context.getString(R.string.error_field_required);
        } else if (!(password.length() >= Constants.MIN_PASSWORD)) {
            error = context.getString(R.string.error_short_password);
        }
        return error;
    }

    public static String isTextValid(Context context, String text) {
        String error = "";
        if (TextUtils.isEmpty(text)) {
            error = context.getString(R.string.error_field_required);
        }
        return error;
    }
}
