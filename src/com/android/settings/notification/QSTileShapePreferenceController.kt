/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.android.settings.notification

import android.content.Context
import android.provider.Settings
import androidx.preference.ListPreference
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.core.BasePreferenceController

/** Controls whether Quick Settings tiles use rounded, square, or state-dependent shapes. */
class QSTileShapePreferenceController(context: Context, key: String) :
    BasePreferenceController(context, key), Preference.OnPreferenceChangeListener {

    override fun getAvailabilityStatus(): Int = AVAILABLE

    override fun updateState(preference: Preference) {
        (preference as ListPreference).value = getTileShape().toString()
        super.updateState(preference)
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        val tileShape = (newValue as? String)?.toIntOrNull() ?: return false
        if (tileShape !in VALID_TILE_SHAPES) {
            return false
        }

        val success =
            Settings.Secure.putInt(
                mContext.contentResolver,
                Settings.Secure.QS_TILE_SHAPE,
                tileShape,
            )
        if (success) {
            refreshSummary(preference)
        }
        return success
    }

    override fun getSummary(): CharSequence {
        val tileShape = getTileShape().toString()
        val values = mContext.resources.getStringArray(R.array.quick_settings_tile_shape_values)
        val entries = mContext.resources.getStringArray(R.array.quick_settings_tile_shape_entries)
        val index = values.indexOf(tileShape)
        return entries[index]
    }

    private fun getTileShape(): Int {
        val value =
            Settings.Secure.getInt(
                mContext.contentResolver,
                Settings.Secure.QS_TILE_SHAPE,
                Settings.Secure.QS_TILE_SHAPE_DEFAULT,
            )
        return value.takeIf { it in VALID_TILE_SHAPES } ?: Settings.Secure.QS_TILE_SHAPE_DEFAULT
    }

    private companion object {
        val VALID_TILE_SHAPES =
            setOf(
                Settings.Secure.QS_TILE_SHAPE_BOTH,
                Settings.Secure.QS_TILE_SHAPE_ROUNDED,
                Settings.Secure.QS_TILE_SHAPE_SQUARE,
            )
    }
}
