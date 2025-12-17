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

package com.android.settings.display

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.preference.ListPreference
import androidx.preference.PreferenceViewHolder
import com.android.settings.R

class DropDownPreference(context: Context, attrs: AttributeSet) : ListPreference(context, attrs) {

    private val adapter: ArrayAdapter<CharSequence> =
        ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item)
    private var spinner: Spinner? = null

    init {
        layoutResource = R.layout.dropdown_preference
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        spinner = holder.findViewById(R.id.spinner) as Spinner
        spinner?.apply {
            adapter = this@DropDownPreference.adapter
            onItemSelectedListener = itemSelectedListener
            setSelection(findIndexOfValue(value))
        }
    }

    override fun setEntries(entries: Array<CharSequence>?) {
        super.setEntries(entries)
        adapter.clear()
        entries?.forEach { entry ->
            adapter.add(entry)
        }
    }

    private val itemSelectedListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            if (position >= 0) {
                val newValue = entryValues[position].toString()
                if (newValue != value) {
                    value = newValue
                    callChangeListener(newValue)
                }
            }
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {
            // No-op
        }
    }
}
