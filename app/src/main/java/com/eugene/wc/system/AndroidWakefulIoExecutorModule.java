package com.eugene.wc.system;

import com.eugene.wc.protocol.api.io.IoExecutor;
import com.eugene.wc.protocol.api.system.AndroidWakeLockManager;
import com.eugene.wc.protocol.api.system.WakefulIoExecutor;

import java.util.concurrent.Executor;

import dagger.Module;
import dagger.Provides;

@Module
public class AndroidWakefulIoExecutorModule {

	@Provides
	@WakefulIoExecutor
	Executor provideWakefulIoExecutor(@IoExecutor Executor ioExecutor,
			AndroidWakeLockManager wakeLockManager) {
		return r -> wakeLockManager.executeWakefully(r, ioExecutor, "WakefulIoExecutor");
	}
}
