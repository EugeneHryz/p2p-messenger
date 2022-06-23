package com.eugene.wc.system;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.logging.Level.INFO;
import static java.util.logging.Logger.getLogger;

import android.os.SystemClock;

import com.eugene.wc.protocol.api.lifecycle.Service;
import com.eugene.wc.protocol.api.system.AndroidWakeLockManager;
import com.eugene.wc.protocol.api.system.TaskScheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

public class AndroidTaskScheduler implements TaskScheduler, Service {

	private static final Logger LOG = getLogger(AndroidTaskScheduler.class.getName());

	private final AndroidWakeLockManager wakeLockManager;
	private final ScheduledExecutorService scheduledExecutorService;

	private final Object lock = new Object();
	private final Queue<ScheduledTask> tasks = new PriorityQueue<>();

	public AndroidTaskScheduler(AndroidWakeLockManager wakeLockManager,
								ScheduledExecutorService scheduledExecutorService) {
		this.wakeLockManager = wakeLockManager;
		this.scheduledExecutorService = scheduledExecutorService;
	}

	@Override
	public void startService() {
	}

	@Override
	public void stopService() {
	}

	@Override
	public Cancellable schedule(Runnable task, Executor executor, long delay,
			TimeUnit unit) {
		AtomicBoolean cancelled = new AtomicBoolean(false);
		return schedule(task, executor, delay, unit, cancelled);
	}

	@Override
	public Cancellable scheduleWithFixedDelay(Runnable task, Executor executor,
			long delay, long interval, TimeUnit unit) {
		AtomicBoolean cancelled = new AtomicBoolean(false);
		return scheduleWithFixedDelay(task, executor, delay, interval, unit,
				cancelled);
	}

	private Cancellable schedule(Runnable task, Executor executor, long delay,
			TimeUnit unit, AtomicBoolean cancelled) {
		long now = SystemClock.elapsedRealtime();
		long dueMillis = now + MILLISECONDS.convert(delay, unit);
		Runnable wakeful = () ->
				wakeLockManager.executeWakefully(task, executor, "TaskHandoff");
		Future<?> check = scheduleCheckForDueTasks(delay, unit);
		ScheduledTask s = new ScheduledTask(wakeful, dueMillis, check,
				cancelled);
		synchronized (lock) {
			tasks.add(s);
		}
		return s;
	}

	private Cancellable scheduleWithFixedDelay(Runnable task, Executor executor,
			long delay, long interval, TimeUnit unit, AtomicBoolean cancelled) {
		// All executions of this periodic task share a cancelled flag
		Runnable wrapped = () -> {
			task.run();
			scheduleWithFixedDelay(task, executor, interval, interval, unit,
					cancelled);
		};
		return schedule(wrapped, executor, delay, unit, cancelled);
	}

	private Future<?> scheduleCheckForDueTasks(long delay, TimeUnit unit) {
		Runnable wakeful = () -> wakeLockManager.runWakefully(
				this::runDueTasks, "TaskScheduler");
		return scheduledExecutorService.schedule(wakeful, delay, unit);
	}

	private void runDueTasks() {
		long now = SystemClock.elapsedRealtime();
		List<ScheduledTask> due = new ArrayList<>();
		synchronized (lock) {
			while (true) {
				ScheduledTask s = tasks.peek();
				if (s == null || s.dueMillis > now) break;
				due.add(tasks.remove());
			}
		}
		if (LOG.isLoggable(INFO)) {
			LOG.info("Running " + due.size() + " due tasks");
		}
		for (ScheduledTask s : due) {
			if (LOG.isLoggable(INFO)) {
				LOG.info("Task is " + (now - s.dueMillis) + " ms overdue");
			}
			s.run();
		}
	}

	private class ScheduledTask
			implements Runnable, Cancellable, Comparable<ScheduledTask> {

		private final Runnable task;
		private final long dueMillis;
		private final Future<?> check;
		private final AtomicBoolean cancelled;

		public ScheduledTask(Runnable task, long dueMillis,
				Future<?> check, AtomicBoolean cancelled) {
			this.task = task;
			this.dueMillis = dueMillis;
			this.check = check;
			this.cancelled = cancelled;
		}

		@Override
		public void run() {
			if (!cancelled.get()) task.run();
		}

		@Override
		public void cancel() {
			// Cancel any future executions of this task
			cancelled.set(true);
			// Cancel the scheduled check for due tasks
			check.cancel(false);
			// Remove the task from the queue
			synchronized (lock) {
				tasks.remove(this);
			}
		}

		@Override
		public int compareTo(ScheduledTask s) {
			//noinspection UseCompareMethod
			if (dueMillis < s.dueMillis) return -1;
			if (dueMillis > s.dueMillis) return 1;
			return 0;
		}
	}
}
