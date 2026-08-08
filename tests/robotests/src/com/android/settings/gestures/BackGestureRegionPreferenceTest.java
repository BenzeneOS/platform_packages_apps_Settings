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

package com.android.settings.gestures;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.preference.PreferenceViewHolder;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.google.android.material.slider.RangeSlider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.text.NumberFormat;

@RunWith(RobolectricTestRunner.class)
public class BackGestureRegionPreferenceTest {
    private Context mContext;
    private BackGestureRegionPreference mPreference;
    private PreferenceViewHolder mViewHolder;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mPreference = new BackGestureRegionPreference(mContext);
        final View view = LayoutInflater.from(mContext).inflate(
                mPreference.getLayoutResource(), new LinearLayout(mContext), false);
        mViewHolder = PreferenceViewHolder.createInstanceForTests(view);
    }

    @Test
    public void setRegion_clampsAndOrdersBounds() {
        mPreference.setRegion(110, -10);

        assertThat(mPreference.getTopPercent()).isEqualTo(0);
        assertThat(mPreference.getBottomPercent()).isEqualTo(100);

        mPreference.setRegion(80, 20);

        assertThat(mPreference.getTopPercent()).isEqualTo(20);
        assertThat(mPreference.getBottomPercent()).isEqualTo(80);
    }

    @Test
    public void onBindViewHolder_setsValuesLabelsAndAccessibility() {
        mPreference.setTitle(R.string.left_edge);
        mPreference.setRegion(15, 75);

        mPreference.onBindViewHolder(mViewHolder);

        final RangeSlider slider =
                (RangeSlider) mViewHolder.findViewById(R.id.back_gesture_region_slider);
        final TextView topLabel =
                (TextView) mViewHolder.findViewById(android.R.id.text1);
        final TextView bottomLabel =
                (TextView) mViewHolder.findViewById(android.R.id.text2);
        final NumberFormat percentFormat = NumberFormat.getPercentInstance(
                mContext.getResources().getConfiguration().getLocales().get(0));
        final String topPercent = percentFormat.format(0.15);
        final String bottomPercent = percentFormat.format(0.75);
        assertThat(slider.getValues()).containsExactly(15f, 75f).inOrder();
        assertThat(topLabel.getText()).isEqualTo(topPercent);
        assertThat(bottomLabel.getText()).isEqualTo(bottomPercent);
        assertThat(slider.getContentDescription()).isEqualTo(
                mContext.getString(R.string.left_edge));
        assertThat(slider.getStateDescription()).isEqualTo(
                mContext.getString(R.string.back_gesture_region_state_description,
                        topPercent, bottomPercent));
    }
}
