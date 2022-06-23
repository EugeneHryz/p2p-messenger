package com.eugene.wc.system;

import static java.util.logging.Level.FINE;
import static java.util.logging.Logger.getLogger;

import androidx.annotation.GuardedBy;

import com.eugene.wc.protocol.api.system.AndroidWakeLock;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * A wrapper around a {@link SharedWakeLock} that provides the more convenient
 * semantics of {@link AndroidWakeLock} (i.e. calls to acquire() and release()
 * don't need to be balanced).
 */
public class AndroidWakeLockImpl implements AndroidWakeLock {

	private static final Logger logger = getLogger(AndroidWakeLockImpl.class.getName());

	private static final AtomicInteger INSTANCE_ID = new AtomicInteger(0);

	private final SharedWakeLock sharedWakeLock;
	private final String tag;

	private final Object lock = new Object();
	@GuardedBy("lock")
	private boolean held = false;

	public AndroidWakeLockImpl(SharedWakeLock sharedWakeLock, String tag) {
		this.sharedWakeLock = sharedWakeLock;
		this.tag = tag + "_" + INSTANCE_ID.getAndIncrement();
	}

	@Override
	public void acquire() {
		synchronized (lock) {
			if (held) {
				if (logger.isLoggable(FINE)) {
					logger.fine(tag + " already acquired");
				}
			} else {
				if (logger.isLoggable(FINE)) {
					logger.fine(tag + " acquiring shared wake lock");
				}
				held = true;
				sharedWakeLock.acquire();
			}
		}
	}

	@Override
	public void release() {
		synchronized (lock) {
			if (held) {
				if (logger.isLoggable(FINE)) {
					logger.fine(tag + " releasing shared wake lock");
				}
				held = false;
				sharedWakeLock.release();
			} else {
				if (logger.isLoggable(FINE)) {
					logger.fine(tag + " already released");
				}
			}
		}
	}
}
