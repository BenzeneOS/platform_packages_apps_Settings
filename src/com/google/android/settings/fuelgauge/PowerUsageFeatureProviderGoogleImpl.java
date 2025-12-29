package com.google.android.settings.fuelgauge;

import android.content.Context;
import android.os.RemoteException;
import android.util.Log;

import com.android.settings.fuelgauge.BatteryInfo;
import com.android.settings.fuelgauge.PowerUsageFeatureProviderImpl;

import vendor.benzeneos.battery.IBattery;

// based on code from SettingsGoogle app
public class PowerUsageFeatureProviderGoogleImpl extends PowerUsageFeatureProviderImpl {
    private static final String TAG = "PowerUsageFeatureProviderGoogleImpl";

    // Property ID for STATE (common across features)
    private static final int PROPERTY_STATE = 18;

    private static final String DWELL_DEFEND_TRIGGER_KEY = "ACTIVE";
    private static final String TEMP_DEFEND_TRIGGER_KEY = " t=1";

    public PowerUsageFeatureProviderGoogleImpl(Context context) {
        super(context);
    }

    @Override
    public boolean isBatteryDefend(BatteryInfo info) {
        IBattery battery = GoogleBattery.getService();
        if (battery == null) {
            return false;
        }

        try {
            String dwellStatus = fetchFeatureStatus(battery, IBattery.Feature.CSI);
            String tempStatus = fetchFeatureStatus(battery, IBattery.Feature.BATTERY_DEFENDER);
            Log.d(TAG, "dwell status: " + dwellStatus + ", temp status: " + tempStatus);
            boolean isDwellDefend = DWELL_DEFEND_TRIGGER_KEY.equals(dwellStatus);
            boolean isTempDefend = tempStatus != null && tempStatus.contains(TEMP_DEFEND_TRIGGER_KEY);
            return isDwellDefend || isTempDefend;
        } catch (Exception e) {
            Log.e(TAG, "", e);
            return false;
        }
    }

    private static String fetchFeatureStatus(IBattery battery, int feature) {
        try {
            return battery.getStringProperty(feature, PROPERTY_STATE);
        } catch (RemoteException e) {
            throw e.rethrowAsRuntimeException();
        }
    }
}
