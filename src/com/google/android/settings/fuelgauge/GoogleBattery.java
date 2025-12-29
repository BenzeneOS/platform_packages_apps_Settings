package com.google.android.settings.fuelgauge;

import android.annotation.Nullable;
import android.os.IBinder;
import android.os.ServiceManager;
import android.util.Log;

import vendor.benzeneos.battery.IBattery;

public class GoogleBattery {
    static final String TAG = "GoogleBattery";

    @Nullable
    public static IBattery getService() {
        String svc = "vendor.benzeneos.battery.IBattery/default";
        IBinder binder = ServiceManager.getService(svc);
        if (binder == null) {
            Log.w(TAG, svc + " is null");
            return null;
        }
        return IBattery.Stub.asInterface(binder);
    }
}
