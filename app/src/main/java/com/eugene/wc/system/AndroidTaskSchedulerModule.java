package com.eugene.wc.system;

import com.eugene.wc.protocol.api.lifecycle.LifecycleManager;
import com.eugene.wc.protocol.api.system.AndroidWakeLockManager;
import com.eugene.wc.protocol.api.system.TaskScheduler;

import java.util.concurrent.ScheduledExecutorService;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class AndroidTaskSchedulerModule {

	public static class EagerSingletons {
		@Inject
        AndroidTaskScheduler scheduler;
	}

	@Provides
	@Singleton
    AndroidTaskScheduler provideAndroidTaskScheduler(LifecycleManager lifecycleManager,
													 AndroidWakeLockManager wakeLockManager,
													 ScheduledExecutorService scheduledExecutorService) {
		AndroidTaskScheduler scheduler = new AndroidTaskScheduler(wakeLockManager, scheduledExecutorService);
		lifecycleManager.registerService(scheduler);
		return scheduler;
	}

	@Provides
	@Singleton
	TaskScheduler provideTaskScheduler(AndroidTaskScheduler scheduler) {
		return scheduler;
	}
}
