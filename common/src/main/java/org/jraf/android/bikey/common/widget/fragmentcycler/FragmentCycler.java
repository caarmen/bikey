/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 * 
 * Copyright (C) 2013 Benoit 'BoD' Lubek (BoD@JRAF.org)
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.jraf.android.bikey.common.widget.fragmentcycler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Checkable;
import android.widget.TextView;

import org.jraf.android.util.handler.HandlerUtil;


public class FragmentCycler {
    private int mContainerResId;
    private List<String> mFragmentTags = new ArrayList<>(10);
    private List<Checkable> mTabs = new ArrayList<>(10);
    private List<Integer> mTitles = new ArrayList<>(10);
    private int mCurrentVisibleIndex = 0;
    private TextView mTxtTitle;
    private Map<String, Boolean> mEnabled = new HashMap<>();
    private long mUpdateTitleDelay;
    private final int mTabColorEnabled;
    private final int mTabColorDisabled;

    public FragmentCycler(int containerResId, TextView txtTitle, long updateTitleDelay, int tabColorEnabled, int tabColorDisabled) {
        mContainerResId = containerResId;
        mTxtTitle = txtTitle;
        mUpdateTitleDelay = updateTitleDelay;
        mTabColorEnabled = tabColorEnabled;
        mTabColorDisabled = tabColorDisabled;
    }

    /**
     * @param tabResId Optional, use 0 for no tab.
     */
    public void add(FragmentActivity activity, Fragment fragment, int tabResId, int titleResId) {
        String tag = getTag(fragment);
        FragmentManager fragmentManager = activity.getSupportFragmentManager();
        Fragment foundFragment = fragmentManager.findFragmentByTag(tag);
        if (foundFragment == null) {
            FragmentTransaction t = fragmentManager.beginTransaction();
            t.add(mContainerResId, fragment, tag);
            t.hide(fragment);
            t.commit();
        } else {
            FragmentTransaction t = fragmentManager.beginTransaction();
            t.hide(foundFragment);
            t.commit();
        }
        mFragmentTags.add(tag);
        View tab = activity.findViewById(tabResId);
        if (tab != null) {
            mTabs.add((Checkable) tab);
            tab.setOnClickListener(mTabOnClickListener);
        }
        mTitles.add(titleResId);
    }

    public void show(FragmentActivity activity) {
        String tag = mFragmentTags.get(mCurrentVisibleIndex);
        FragmentManager fragmentManager = activity.getSupportFragmentManager();
        fragmentManager.executePendingTransactions();
        Fragment fragment = fragmentManager.findFragmentByTag(tag);
        FragmentTransaction t = fragmentManager.beginTransaction();
        t.show(fragment);
        t.commit();
        if (!mTabs.isEmpty()) {
            Checkable checkable = mTabs.get(mCurrentVisibleIndex);
            checkable.setChecked(true);
        }
        updateTitle();
    }

    public void cycle(FragmentActivity activity) {
        // Find the next *enabled* fragment to show
        int newIndex = mCurrentVisibleIndex;
        do {
            newIndex = (newIndex + 1) % mFragmentTags.size();
        } while (!isEnabled(newIndex));
        setCurrentVisibleIndex(activity, newIndex);
    }

    private void setCurrentVisibleIndex(FragmentActivity activity, int newIndex) {
        int previousVisibleIndex = mCurrentVisibleIndex;
        mCurrentVisibleIndex = newIndex;
        String hideTag = mFragmentTags.get(previousVisibleIndex);
        String showTag = mFragmentTags.get(mCurrentVisibleIndex);
        FragmentManager fragmentManager = activity.getSupportFragmentManager();
        Fragment showFragment = fragmentManager.findFragmentByTag(showTag);
        Fragment hideFragment = fragmentManager.findFragmentByTag(hideTag);
        FragmentTransaction t = fragmentManager.beginTransaction();
        t.hide(hideFragment);
        t.show(showFragment);
        t.commit();
        if (!mTabs.isEmpty()) {
            Checkable prevCheckable = mTabs.get(previousVisibleIndex);
            prevCheckable.setChecked(false);
            Checkable curCheckable = mTabs.get(mCurrentVisibleIndex);
            curCheckable.setChecked(true);
        }
        updateTitle();
    }

    private void updateTitle() {
        HandlerUtil.getMainHandler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mTxtTitle.setText(mTitles.get(mCurrentVisibleIndex));
            }
        }, mUpdateTitleDelay);
    }

    private String getTag(Fragment fragment) {
        return getTag(fragment.getClass());
    }

    private String getTag(Class<? extends Fragment> fragmentClass) {
        return fragmentClass.getName();
    }

    private OnClickListener mTabOnClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!(v instanceof Checkable)) return;
            Checkable checkable = (Checkable) v;
            if (!checkable.isChecked()) checkable.setChecked(true);
            int newIndex = mTabs.indexOf(checkable);
            if (mCurrentVisibleIndex == newIndex) return;
            setCurrentVisibleIndex((FragmentActivity) v.getContext(), newIndex);
        }
    };

    public int getCurrentVisibleIndex() {
        return mCurrentVisibleIndex;
    }

    public void setCurrentVisibleIndex(int currentVisibleIndex) {
        mCurrentVisibleIndex = currentVisibleIndex;
    }

    public void setEnabled(Context context, Class<? extends Fragment> fragmentClass, boolean enabled) {
        String tag = getTag(fragmentClass);
        mEnabled.put(tag, enabled);

        int index = mFragmentTags.indexOf(tag);
        if (!mTabs.isEmpty()) {
            TextView textView = (TextView) mTabs.get(index);
            textView.setTextColor(enabled ? mTabColorEnabled : mTabColorDisabled);
        }
    }

    private boolean isEnabled(int index) {
        Boolean enabled = mEnabled.get(mFragmentTags.get(index));
        if (enabled == null) {
            // Treat no value (default) as enabled.
            enabled = true;
        }
        return enabled;
    }
}
