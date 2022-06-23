package com.eugene.wc.util;

import static java.util.Objects.requireNonNull;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.provider.Settings;

import com.eugene.wc.protocol.api.Pair;
import com.eugene.wc.protocol.api.util.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class AndroidUtils {

	// Fake Bluetooth address returned by BluetoothAdapter on API 23 and later
	private static final String FAKE_BLUETOOTH_ADDRESS = "02:00:00:00:00:00";

	public static String getBluetoothAddress(Context ctx, BluetoothAdapter adapter) {
		return getBluetoothAddressAndMethod(ctx, adapter).getFirst();
	}

	public static Pair<String, String> getBluetoothAddressAndMethod(Context ctx,
																	BluetoothAdapter adapter) {
		// Return the adapter's address if it's valid and not fake
		@SuppressLint("HardwareIds")
		String address = adapter.getAddress();
		if (isValidBluetoothAddress(address)) {
			return new Pair<>(address, "adapter");
		}
		// Return the address from settings if it's valid and not fake
		address = Settings.Secure.getString(ctx.getContentResolver(),
				"bluetooth_address");
		if (isValidBluetoothAddress(address)) {
			return new Pair<>(address, "settings");
		}
		// Try to get the address via reflection
		address = getBluetoothAddressByReflection(adapter);
		if (isValidBluetoothAddress(address)) {
			return new Pair<>(requireNonNull(address), "reflection");
		}
		// Let the caller know we can't find the address
		return new Pair<>("", "");
	}

	public static boolean isValidBluetoothAddress(String address) {
		return !StringUtils.isNullOrEmpty(address)
				&& BluetoothAdapter.checkBluetoothAddress(address)
				&& !address.equals(FAKE_BLUETOOTH_ADDRESS);
	}

	private static String getBluetoothAddressByReflection(BluetoothAdapter adapter) {
		try {
			@SuppressLint("DiscouragedPrivateApi")
			Field mServiceField = adapter.getClass().getDeclaredField("mService");
			mServiceField.setAccessible(true);
			Object mService = mServiceField.get(adapter);
			// mService may be null when Bluetooth is disabled
			if (mService == null) throw new NoSuchFieldException();
			Method getAddressMethod =
					mService.getClass().getMethod("getAddress");
			return (String) getAddressMethod.invoke(mService);
		} catch (NoSuchFieldException | InvocationTargetException
				| IllegalAccessException | NoSuchMethodException
				| SecurityException e) {
			return null;
		}
	}
}
