package com.eugene.wc.protocol.api.settings;

import com.eugene.wc.protocol.api.db.exception.DbException;

import java.sql.Connection;

public interface SettingsManager {

	/**
	 * Returns all settings in the given namespace.
	 */
	Settings getSettings(String namespace) throws DbException;

	/**
	 * Returns all settings in the given namespace.
	 */
	Settings getSettings(Connection txn, String namespace) throws DbException;

	/**
	 * Merges the given settings with any existing settings in the given
	 * namespace.
	 */
	void mergeSettings(Settings s, String namespace) throws DbException;

	/**
	 * Merges the given settings with any existing settings in the given
	 * namespace.
	 */
	void mergeSettings(Connection txn, Settings s, String namespace)
			throws DbException;
}
