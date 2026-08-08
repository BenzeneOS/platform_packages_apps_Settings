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
import com.android.settings.R
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class QSTileShapePreferenceControllerTest {

    private lateinit var context: Context
    private lateinit var controller: QSTileShapePreferenceController
    private lateinit var preference: ListPreference

    @Before
    fun setUp() {
        context = RuntimeEnvironment.application
        Settings.Secure.putString(context.contentResolver, Settings.Secure.QS_TILE_SHAPE, null)
        controller = QSTileShapePreferenceController(context, KEY)
        preference =
            ListPreference(context).apply {
                key = KEY
                entries =
                    context.resources.getStringArray(R.array.quick_settings_tile_shape_entries)
                entryValues =
                    context.resources.getStringArray(R.array.quick_settings_tile_shape_values)
            }
    }

    @Test
    fun updateState_noStoredValue_usesBothByDefault() {
        controller.updateState(preference)

        assertThat(preference.value).isEqualTo(Settings.Secure.QS_TILE_SHAPE_BOTH.toString())
        assertThat(preference.summary)
            .isEqualTo(context.getString(R.string.quick_settings_tile_shape_both))
    }

    @Test
    fun onPreferenceChange_rounded_persistsAndUpdatesSummary() {
        val success =
            controller.onPreferenceChange(
                preference,
                Settings.Secure.QS_TILE_SHAPE_ROUNDED.toString(),
            )

        assertThat(success).isTrue()
        assertThat(
                Settings.Secure.getInt(
                    context.contentResolver,
                    Settings.Secure.QS_TILE_SHAPE,
                    Settings.Secure.QS_TILE_SHAPE_DEFAULT,
                )
            )
            .isEqualTo(Settings.Secure.QS_TILE_SHAPE_ROUNDED)
        assertThat(preference.summary)
            .isEqualTo(context.getString(R.string.quick_settings_tile_shape_rounded))
    }

    @Test
    fun onPreferenceChange_square_persistsAndUpdatesSummary() {
        val success =
            controller.onPreferenceChange(
                preference,
                Settings.Secure.QS_TILE_SHAPE_SQUARE.toString(),
            )

        assertThat(success).isTrue()
        assertThat(
                Settings.Secure.getInt(
                    context.contentResolver,
                    Settings.Secure.QS_TILE_SHAPE,
                    Settings.Secure.QS_TILE_SHAPE_DEFAULT,
                )
            )
            .isEqualTo(Settings.Secure.QS_TILE_SHAPE_SQUARE)
        assertThat(preference.summary)
            .isEqualTo(context.getString(R.string.quick_settings_tile_shape_square))
    }

    @Test
    fun onPreferenceChange_invalidValue_doesNotPersist() {
        val success = controller.onPreferenceChange(preference, "invalid")

        assertThat(success).isFalse()
        assertThat(
                Settings.Secure.getInt(
                    context.contentResolver,
                    Settings.Secure.QS_TILE_SHAPE,
                    Settings.Secure.QS_TILE_SHAPE_DEFAULT,
                )
            )
            .isEqualTo(Settings.Secure.QS_TILE_SHAPE_DEFAULT)
    }

    private companion object {
        const val KEY = "quick_settings_tile_shape"
    }
}
