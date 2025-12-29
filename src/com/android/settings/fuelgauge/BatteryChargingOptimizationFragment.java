package com.android.settings.fuelgauge;

import android.content.Context;
import android.ext.power.BatteryChargeLimit;
import android.ext.settings.BoolSetting;
import android.icu.text.MessageFormat;
import android.icu.text.NumberFormat;
import android.os.RemoteException;
import android.util.Log;

import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.ext.BoolSettingFragment;
import com.android.settingslib.widget.FooterPreference;
import com.android.settingslib.widget.SliderPreference;
import com.google.android.settings.fuelgauge.GoogleBattery;

import vendor.benzeneos.battery.IBattery;

public class BatteryChargingOptimizationFragment extends BoolSettingFragment {
	private static final String TAG = "BatteryChargingOptimizationFragment";
	private static final String KEY_CHARGE_LEVEL = "charge_level";

	private SliderPreference mChargeLevelPref;
	private FooterPreference footer;

	@Override
	protected void addExtraPrefs(PreferenceScreen screen) {
		Context context = requireContext();

		// Add charge level slider
		mChargeLevelPref = new SliderPreference(context);
		mChargeLevelPref.setKey(KEY_CHARGE_LEVEL);
		mChargeLevelPref.setTitle(R.string.charging_optimization_level_title);
		mChargeLevelPref.setMin(BatteryChargeLimit.MIN_CHARGE_LEVEL);
		mChargeLevelPref.setMax(BatteryChargeLimit.MAX_CHARGE_LEVEL);
		mChargeLevelPref.setValue(BatteryChargeLimit.getChargeStopLevel(context));
		mChargeLevelPref.setShowSliderValue(true);
		// Force integer steps and show integer percentage in label
		mChargeLevelPref.setSliderIncrement(1);
		mChargeLevelPref.setLabelFormater(value -> String.format("%d%%", (int) value));
		// Enable haptic feedback on each step
		mChargeLevelPref.setHapticFeedbackMode(SliderPreference.HAPTIC_FEEDBACK_MODE_ON_TICKS);
		mChargeLevelPref.setOnPreferenceChangeListener((preference, newValue) -> {
			int stopLevel = (Integer) newValue;
			int startLevel = stopLevel - BatteryChargeLimit.DEFAULT_LEVEL_GAP;
			// Kernel requires start level >= 49
			if (startLevel < BatteryChargeLimit.MIN_START_LEVEL) {
				startLevel = BatteryChargeLimit.MIN_START_LEVEL;
			}
			// Return true on success - SliderPreference.syncValueInternal() will update
			// mSliderValue.
			// Do NOT call setValue() here as it triggers notifyChanged() causing async
			// rebind/bounce.
			return applyChargeLevels(stopLevel, startLevel);
		});

		screen.addPreference(mChargeLevelPref);

		// Update visibility based on current state
		updateChargeLevelVisibility(getSetting().get(context));
	}

	@Override
	protected boolean interceptMainSwitchChange(boolean newValue) {
		Context context = requireContext();
		int stopLevel = BatteryChargeLimit.getChargeStopLevel(context);
		int startLevel = BatteryChargeLimit.getChargeStartLevel(context);

		if (newValue) {
			// When enabling, apply current charge levels
			if (!applyChargeLevels(stopLevel, startLevel)) {
				return true; // block if failed
			}
		} else {
			// When disabling, set policy to DEFAULT
			if (!setChargingPolicy(IBattery.ChargingPolicy.DEFAULT)) {
				return true; // block if failed
			}
		}

		return false; // allow setting update
	}

	/**
	 * Apply custom charge levels to the HAL.
	 */
	private boolean applyChargeLevels(int stopLevel, int startLevel) {
		Context context = requireContext();

		// Apply to HAL first — only persist settings on success
		IBattery service = GoogleBattery.getService();
		if (service == null) {
			Log.e(TAG, "GoogleBattery service not available");
			return false;
		}

		try {
			service.setChargeLimit(stopLevel, startLevel);
			Log.d(TAG, "setChargeLimit: stop=" + stopLevel + ", start=" + startLevel);

			int policy = (stopLevel == BatteryChargeLimit.DEFAULT_CHARGE_LEVEL)
					? IBattery.ChargingPolicy.LONGLIFE
					: IBattery.ChargingPolicy.CUSTOM;
			service.setChargingPolicy(policy);
		} catch (RemoteException e) {
			Log.e(TAG, "Failed to apply charge levels", e);
			return false;
		}

		// HAL succeeded — now persist to settings
		BatteryChargeLimit.setChargeLevels(context, stopLevel, startLevel);
		updateMainSwitchTitle(stopLevel);
		updateFooter(stopLevel);
		return true;
	}

	static boolean setChargingPolicy(int policy) {
		IBattery service = GoogleBattery.getService();
		if (service == null) {
			return false;
		}
		try {
			service.setChargingPolicy(policy);
			Log.d(TAG, "setChargingPolicy to " + policy);
			return true;
		} catch (RemoteException e) {
			Log.e(TAG, "", e);
			return false;
		}
	}

	@Override
	protected BoolSetting getSetting() {
		return BatteryChargeLimit.getSetting();
	}

	@Override
	protected CharSequence getTitle() {
		return getText(R.string.charging_optimization_title);
	}

	@Override
	protected CharSequence getMainSwitchTitle() {
		int chargeLevel = BatteryChargeLimit.getChargeStopLevel(requireContext());
		return requireContext().getString(
				R.string.charging_optimization_entry_summary_charge_limit,
				NumberFormat.getPercentInstance().format(chargeLevel / 100f));
	}

	@Override
	protected void onMainSwitchChanged(boolean state) {
		updateChargeLevelVisibility(state);
		if (footer != null) {
			footer.setVisible(state);
		}
	}

	private void updateChargeLevelVisibility(boolean enabled) {
		if (mChargeLevelPref != null) {
			mChargeLevelPref.setVisible(enabled);
		}
	}

	private void updateMainSwitchTitle(int stopLevel) {
		if (mainSwitch != null) {
			mainSwitch.setTitle(requireContext().getString(
					R.string.charging_optimization_entry_summary_charge_limit,
					NumberFormat.getPercentInstance().format(stopLevel / 100f)));
		}
	}

	private void updateFooter(int stopLevel) {
		if (footer != null) {
			String text = requireContext().getString(
					R.string.charging_optimization_footer_message_charge_limit,
					NumberFormat.getPercentInstance().format(stopLevel / 100f),
					NumberFormat.getPercentInstance().format(1f));
			footer.setTitle(text);
		}
	}

	@Override
	protected FooterPreference makeFooterPref(FooterPreference.Builder builder) {
		int chargeLevel = BatteryChargeLimit.getChargeStopLevel(requireContext());
		String text = requireContext().getString(
				R.string.charging_optimization_footer_message_charge_limit,
				NumberFormat.getPercentInstance().format(chargeLevel / 100f),
				NumberFormat.getPercentInstance().format(1f));

		builder.setTitle(text);

		FooterPreference pref = builder.build();
		pref.setVisible(getSetting().get(requireContext()));
		this.footer = pref;
		return pref;
	}
}
