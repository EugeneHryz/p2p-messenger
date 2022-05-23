package com.eugene.wc.protocol.settings;

import com.eugene.wc.protocol.api.db.exception.DbException;
import com.eugene.wc.protocol.api.settings.Settings;
import com.eugene.wc.protocol.api.settings.SettingsManager;

import java.sql.Connection;

import javax.inject.Inject;

class SettingsManagerImpl implements SettingsManager {

//	private final DatabaseComponent db;

	@Inject
	SettingsManagerImpl() {
	}

	@Override
	public Settings getSettings(String namespace) throws DbException {
//		return db.transactionWithResult(true, txn ->
//				db.getSettings(txn, namespace));
		return new Settings();
	}

	@Override
	public Settings getSettings(Connection txn, String namespace) throws DbException {
//		return db.getSettings(txn, namespace);
		return new Settings();
	}

	@Override
	public void mergeSettings(Settings s, String namespace) throws DbException {
//		db.transaction(false, txn -> db.mergeSettings(txn, s, namespace));
	}

	@Override
	public void mergeSettings(Connection txn, Settings s, String namespace) throws DbException {
//		db.mergeSettings(txn, s, namespace);
	}
}
