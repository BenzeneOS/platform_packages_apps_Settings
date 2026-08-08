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

import static android.view.HapticFeedbackConstants.CLOCK_TICK;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.google.android.material.slider.LabelFormatter;
import com.google.android.material.slider.RangeSlider;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * A preference that displays a two-ended slider for selecting a vertical back gesture region.
 */
public class BackGestureRegionPreference extends Preference {
    private static final String TAG = "BackGestureRegionPref";
    private static final int MIN_PERCENT = 0;
    private static final int MAX_PERCENT = 100;

    @Nullable
    private RangeSlider mSlider;
    @Nullable
    private TextView mTopLabel;
    @Nullable
    private TextView mBottomLabel;
    private int mTopPercent = MIN_PERCENT;
    private int mBottomPercent = MAX_PERCENT;

    private final View.OnKeyListener mSliderKeyListener = new View.OnKeyListener() {
        @Override
        public boolean onKey(@NonNull View v, int keyCode, @NonNull KeyEvent event) {
            if (event.getAction() != KeyEvent.ACTION_DOWN) {
                return false;
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
                return false;
            }
            if (mSlider == null) {
                Log.e(TAG, "RangeSlider view is null and cannot be adjusted.");
                return false;
            }
            return mSlider.onKeyDown(keyCode, event);
        }
    };

    private final RangeSlider.OnChangeListener mChangeListener =
            new RangeSlider.OnChangeListener() {
                @Override
                public void onValueChange(@NonNull RangeSlider slider, float value,
                        boolean fromUser) {
                    if (fromUser) {
                        syncValueInternal(slider);
                    }
                }
            };

    public BackGestureRegionPreference(@NonNull Context context, @Nullable AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setLayoutResource(R.layout.back_gesture_region_preference);
    }

    public BackGestureRegionPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0 /* defStyleAttr */);
    }

    public BackGestureRegionPreference(@NonNull Context context) {
        this(context, null);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        holder.itemView.setOnKeyListener(mSliderKeyListener);
        holder.itemView.setClickable(false);

        mSlider = (RangeSlider) holder.findViewById(R.id.back_gesture_region_slider);
        mTopLabel = (TextView) holder.findViewById(android.R.id.text1);
        mBottomLabel = (TextView) holder.findViewById(android.R.id.text2);
        if (mSlider == null) {
            Log.e(TAG, "RangeSlider is null in onBindViewHolder.");
            return;
        }

        mSlider.setValueFrom(MIN_PERCENT);
        mSlider.setValueTo(MAX_PERCENT);
        mSlider.setStepSize(1);
        mSlider.setLabelBehavior(LabelFormatter.LABEL_GONE);
        final CharSequence title = getTitle();
        mSlider.setContentDescription(TextUtils.isEmpty(title) ? null : title);
        mSlider.setValues((float) mTopPercent, (float) mBottomPercent);
        mSlider.setStateDescription(formatStateDescription());
        mSlider.clearOnChangeListeners();
        mSlider.addOnChangeListener(mChangeListener);
        mSlider.setEnabled(isEnabled());
        mSlider.setClickable(isSelectable());
        updateLabelViews();
    }

    public void setRegion(int topPercent, int bottomPercent) {
        setRegionInternal(topPercent, bottomPercent, true);
    }

    public int getTopPercent() {
        return mTopPercent;
    }

    public int getBottomPercent() {
        return mBottomPercent;
    }

    private void syncValueInternal(@NonNull RangeSlider slider) {
        final List<Float> values = slider.getValues();
        if (values.size() < 2) {
            return;
        }
        final int topPercent = clamp(Math.round(values.get(0)));
        final int bottomPercent = clamp(Math.round(values.get(values.size() - 1)));
        if (topPercent == mTopPercent && bottomPercent == mBottomPercent) {
            return;
        }

        if (callChangeListener(new int[] {topPercent, bottomPercent})) {
            setRegionInternal(topPercent, bottomPercent, false);
            slider.performHapticFeedback(CLOCK_TICK);
        } else {
            slider.setValues((float) mTopPercent, (float) mBottomPercent);
        }
    }

    private void setRegionInternal(int topPercent, int bottomPercent, boolean notifyChanged) {
        final int newTopPercent = clamp(Math.min(topPercent, bottomPercent));
        final int newBottomPercent = clamp(Math.max(topPercent, bottomPercent));
        if (newTopPercent == mTopPercent && newBottomPercent == mBottomPercent) {
            return;
        }

        mTopPercent = newTopPercent;
        mBottomPercent = newBottomPercent;
        if (mSlider != null) {
            mSlider.setValues((float) mTopPercent, (float) mBottomPercent);
            mSlider.setStateDescription(formatStateDescription());
        }
        updateLabelViews();
        if (notifyChanged) {
            notifyChanged();
        }
    }

    private void updateLabelViews() {
        if (mTopLabel != null) {
            mTopLabel.setText(formatPercent(mTopPercent));
        }
        if (mBottomLabel != null) {
            mBottomLabel.setText(formatPercent(mBottomPercent));
        }
    }

    private String formatStateDescription() {
        return getContext().getString(R.string.back_gesture_region_state_description,
                formatPercent(mTopPercent), formatPercent(mBottomPercent));
    }

    private String formatPercent(int percent) {
        final Locale locale = getContext().getResources().getConfiguration().getLocales().get(0);
        return NumberFormat.getPercentInstance(locale).format(percent / (double) MAX_PERCENT);
    }

    private static int clamp(int value) {
        return Math.max(MIN_PERCENT, Math.min(MAX_PERCENT, value));
    }
}
