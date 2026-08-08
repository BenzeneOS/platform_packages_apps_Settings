/*
 * Copyright (C) 2026 The BenzeneOS Authors
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

package com.android.settings.fuelgauge.batteryusage;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.UserHandle;

import com.android.settings.core.BasePreferenceController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class ObservatoryPreferenceControllerTest {
    private static final String PREFERENCE_KEY = "observatory";
    private static final int SECONDARY_USER_ID = 10;
    private static final ComponentName OBSERVATORY_COMPONENT =
            new ComponentName(
                    "org.benzeneos.observatory", "org.benzeneos.observatory.MainActivity");

    @Mock private Context mContext;
    @Mock private PackageManager mPackageManager;

    private ObservatoryPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mContext.getUserId()).thenReturn(UserHandle.USER_SYSTEM);
        mController = new ObservatoryPreferenceController(mContext, PREFERENCE_KEY);
    }

    @Test
    public void getAvailabilityStatus_systemUserWithActivity_returnsAvailable() {
        when(mPackageManager.resolveActivity(any(Intent.class), eq(0)))
                .thenReturn(new ResolveInfo());

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
        final ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(mPackageManager).resolveActivity(intentCaptor.capture(), eq(0));
        assertThat(intentCaptor.getValue().getAction()).isEqualTo(Intent.ACTION_MAIN);
        assertThat(intentCaptor.getValue().getComponent()).isEqualTo(OBSERVATORY_COMPONENT);
    }

    @Test
    public void getAvailabilityStatus_missingActivity_returnsUnsupported() {
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_secondaryUserWithActivity_returnsUnsupported() {
        when(mContext.getUserId()).thenReturn(SECONDARY_USER_ID);
        when(mPackageManager.resolveActivity(any(Intent.class), eq(0)))
                .thenReturn(new ResolveInfo());

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.UNSUPPORTED_ON_DEVICE);
        verify(mPackageManager, never()).resolveActivity(any(Intent.class), eq(0));
    }

    @Test
    public void updateNonIndexableKeys_missingActivity_addsKey() {
        final List<String> keys = new ArrayList<>();

        mController.updateNonIndexableKeys(keys);

        assertThat(keys).containsExactly(PREFERENCE_KEY);
    }

    @Test
    public void updateNonIndexableKeys_availableActivity_doesNotAddKey() {
        when(mPackageManager.resolveActivity(any(Intent.class), eq(0)))
                .thenReturn(new ResolveInfo());
        final List<String> keys = new ArrayList<>();

        mController.updateNonIndexableKeys(keys);

        assertThat(keys).isEmpty();
    }
}
