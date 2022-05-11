package com.eugene.wc.protocol.plugin;

import com.eugene.wc.protocol.api.plugin.Backoff;
import com.eugene.wc.protocol.api.plugin.BackoffFactory;

import javax.annotation.concurrent.Immutable;

@Immutable
class BackoffFactoryImpl implements BackoffFactory {

	@Override
	public Backoff createBackoff(int minInterval, int maxInterval,
								 double base) {
		return new BackoffImpl(minInterval, maxInterval, base);
	}
}
