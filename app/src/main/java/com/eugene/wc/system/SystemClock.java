package com.eugene.wc.system;

import com.eugene.wc.protocol.api.system.Clock;

/**
 * Default clock implementation.
 */
public class SystemClock implements Clock {

	@Override
	public long currentTimeMillis() {
		return System.currentTimeMillis();
	}

	@Override
	public void sleep(long milliseconds) throws InterruptedException {
		Thread.sleep(milliseconds);
	}
}
