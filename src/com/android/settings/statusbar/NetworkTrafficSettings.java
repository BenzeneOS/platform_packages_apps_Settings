/*
 * Copyright (C) 2025 Amaan Qureshi <contact@amaanq.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.statusbar;

import android.app.settings.SettingsEnums;
import android.content.ContentResolver;
import android.os.Bundle;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import app.benzeneos.providers.BenzeneSettings;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

@SearchIndexable
public class NetworkTrafficSettings extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    private static final String KEY_ENABLED = "network_traffic_enabled";
    private static final String KEY_MODE = "network_traffic_mode";
    private static final String KEY_AUTOHIDE = "network_traffic_autohide";
    private static final String KEY_UNITS = "network_traffic_units";
    private static final String KEY_REFRESH_INTERVAL = "network_traffic_refresh_interval";
    private static final String KEY_HIDE_ARROW = "network_traffic_hidearrow";

    private SwitchPreferenceCompat mEnabledPref;
    private ListPreference mModePref;
    private SwitchPreferenceCompat mAutohidePref;
    private ListPreference mUnitsPref;
    private ListPreference mRefreshIntervalPref;
    private SwitchPreferenceCompat mHideArrowPref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.network_traffic_settings);

        final ContentResolver resolver = getContentResolver();

        mEnabledPref = findPreference(KEY_ENABLED);
        mEnabledPref.setChecked(BenzeneSettings.System.getInt(resolver,
                BenzeneSettings.System.NETWORK_TRAFFIC_ENABLED, 0) == 1);
        mEnabledPref.setOnPreferenceChangeListener(this);

        mModePref = findPreference(KEY_MODE);
        int mode = BenzeneSettings.System.getInt(resolver,
                BenzeneSettings.System.NETWORK_TRAFFIC_MODE, 0);
        mModePref.setValue(String.valueOf(mode));
        mModePref.setSummary(mModePref.getEntry());
        mModePref.setOnPreferenceChangeListener(this);

        mAutohidePref = findPreference(KEY_AUTOHIDE);
        mAutohidePref.setChecked(BenzeneSettings.System.getInt(resolver,
                BenzeneSettings.System.NETWORK_TRAFFIC_AUTOHIDE, 0) == 1);
        mAutohidePref.setOnPreferenceChangeListener(this);

        mUnitsPref = findPreference(KEY_UNITS);
        int units = BenzeneSettings.System.getInt(resolver,
                BenzeneSettings.System.NETWORK_TRAFFIC_UNITS, 1);
        mUnitsPref.setValue(String.valueOf(units));
        mUnitsPref.setSummary(mUnitsPref.getEntry());
        mUnitsPref.setOnPreferenceChangeListener(this);

        mRefreshIntervalPref = findPreference(KEY_REFRESH_INTERVAL);
        int interval = BenzeneSettings.System.getInt(resolver,
                BenzeneSettings.System.NETWORK_TRAFFIC_REFRESH_INTERVAL, 2);
        mRefreshIntervalPref.setValue(String.valueOf(interval));
        mRefreshIntervalPref.setSummary(mRefreshIntervalPref.getEntry());
        mRefreshIntervalPref.setOnPreferenceChangeListener(this);

        mHideArrowPref = findPreference(KEY_HIDE_ARROW);
        mHideArrowPref.setChecked(BenzeneSettings.System.getInt(resolver,
                BenzeneSettings.System.NETWORK_TRAFFIC_HIDEARROW, 0) == 1);
        mHideArrowPref.setOnPreferenceChangeListener(this);

        updateDependencies(mEnabledPref.isChecked());
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final ContentResolver resolver = getContentResolver();

        if (preference == mEnabledPref) {
            boolean enabled = (Boolean) newValue;
            BenzeneSettings.System.putInt(resolver,
                    BenzeneSettings.System.NETWORK_TRAFFIC_ENABLED, enabled ? 1 : 0);
            updateDependencies(enabled);
            return true;
        } else if (preference == mModePref) {
            int mode = Integer.parseInt((String) newValue);
            BenzeneSettings.System.putInt(resolver,
                    BenzeneSettings.System.NETWORK_TRAFFIC_MODE, mode);
            int index = mModePref.findIndexOfValue((String) newValue);
            mModePref.setSummary(mModePref.getEntries()[index]);
            return true;
        } else if (preference == mAutohidePref) {
            boolean autohide = (Boolean) newValue;
            BenzeneSettings.System.putInt(resolver,
                    BenzeneSettings.System.NETWORK_TRAFFIC_AUTOHIDE, autohide ? 1 : 0);
            return true;
        } else if (preference == mUnitsPref) {
            int units = Integer.parseInt((String) newValue);
            BenzeneSettings.System.putInt(resolver,
                    BenzeneSettings.System.NETWORK_TRAFFIC_UNITS, units);
            int index = mUnitsPref.findIndexOfValue((String) newValue);
            mUnitsPref.setSummary(mUnitsPref.getEntries()[index]);
            return true;
        } else if (preference == mRefreshIntervalPref) {
            int interval = Integer.parseInt((String) newValue);
            BenzeneSettings.System.putInt(resolver,
                    BenzeneSettings.System.NETWORK_TRAFFIC_REFRESH_INTERVAL, interval);
            int index = mRefreshIntervalPref.findIndexOfValue((String) newValue);
            mRefreshIntervalPref.setSummary(mRefreshIntervalPref.getEntries()[index]);
            return true;
        } else if (preference == mHideArrowPref) {
            boolean hideArrow = (Boolean) newValue;
            BenzeneSettings.System.putInt(resolver,
                    BenzeneSettings.System.NETWORK_TRAFFIC_HIDEARROW, hideArrow ? 1 : 0);
            return true;
        }
        return false;
    }

    private void updateDependencies(boolean enabled) {
        mModePref.setEnabled(enabled);
        mAutohidePref.setEnabled(enabled);
        mUnitsPref.setEnabled(enabled);
        mRefreshIntervalPref.setEnabled(enabled);
        mHideArrowPref.setEnabled(enabled);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DISPLAY;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.network_traffic_settings);
}
