package com.bilko.findme.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.BaseAdapter;

abstract class AbstractAdapter extends BaseAdapter {

    final LayoutInflater mLayoutInflater;

    public AbstractAdapter(final Context context) {
        mLayoutInflater = LayoutInflater.from(context);
    }
}
