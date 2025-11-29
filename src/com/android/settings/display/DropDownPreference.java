/*
 * Copyright (C) 2025 GrapheneOS
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

package com.android.settings.display;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.preference.ListPreference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;

public class DropDownPreference extends ListPreference {

    private ArrayAdapter<CharSequence> mAdapter;
    private Spinner mSpinner;

    public DropDownPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.dropdown_preference);
        mAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        mSpinner = (Spinner) holder.findViewById(R.id.spinner);
        mSpinner.setAdapter(mAdapter);
        mSpinner.setOnItemSelectedListener(mItemSelectedListener);
        mSpinner.setSelection(findIndexOfValue(getValue()));
    }

    @Override
    public void setEntries(CharSequence[] entries) {
        super.setEntries(entries);
        mAdapter.clear();
        for (CharSequence entry : entries) {
            mAdapter.add(entry);
        }
    }

    private final AdapterView.OnItemSelectedListener mItemSelectedListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            if (position >= 0) {
                String value = getEntryValues()[position].toString();
                if (!value.equals(getValue())) {
                    setValue(value);
                    callChangeListener(value);
                }
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            // No-op
        }
    };
}
