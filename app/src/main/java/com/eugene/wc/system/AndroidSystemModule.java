package com.eugene.wc.system;

import com.eugene.wc.protocol.api.lifecycle.EventExecutor;
import com.eugene.wc.protocol.api.lifecycle.LifecycleManager;
import com.eugene.wc.protocol.api.system.AndroidExecutor;
import com.eugene.wc.protocol.api.system.AndroidWakeLockManager;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class AndroidSystemModule {

	private final ScheduledExecutorService scheduledExecutorService;

	public AndroidSystemModule() {
		// Discard tasks that are submitted during shutdown
		RejectedExecutionHandler policy =
				new ScheduledThreadPoolExecutor.DiscardPolicy();
		scheduledExecutorService = new ScheduledThreadPoolExecutor(1, policy);
	}

	@Provides
	@Singleton
	ScheduledExecutorService provideScheduledExecutorService(
			LifecycleManager lifecycleManager) {
		lifecycleManager.registerForShutdown(scheduledExecutorService);
		return scheduledExecutorService;
	}

	@Provides
	@Singleton
	AndroidExecutor provideAndroidExecutor(
			AndroidExecutorImpl androidExecutor) {
		return androidExecutor;
	}

	@Provides
	@Singleton
	@EventExecutor
	Executor provideEventExecutor(AndroidExecutor androidExecutor) {
		return androidExecutor::runOnUiThread;
	}

//	@Provides
//	@Singleton
//	@TestExecutor
//	Executor provideTestExecutor() {
//		RejectedExecutionHandler reh = new ThreadPoolExecutor.DiscardPolicy();
//		BlockingQueue<Runnable> taskQueue = new LinkedBlockingQueue<>();
//
//		return new ThreadPoolExecutor(4, 8, 30,
//				TimeUnit.SECONDS, taskQueue, reh);
//	}

//	@Provides
//	@Singleton
//	ResourceProvider provideResourceProvider(org.briarproject.bramble.system.AndroidResourceProvider provider) {
//		return provider;
//	}

	@Provides
	@Singleton
	AndroidWakeLockManager provideWakeLockManager(
			AndroidWakeLockManagerImpl wakeLockManager) {
		return wakeLockManager;
	}
}
