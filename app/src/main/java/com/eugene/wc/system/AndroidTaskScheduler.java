package com.eugene.wc.system;

import static android.app.AlarmManager.INTERVAL_FIFTEEN_MINUTES;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.logging.Level.INFO;
import static java.util.logging.Logger.getLogger;

import android.app.Application;
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

	private static final Logger LOG =
			getLogger(AndroidTaskScheduler.class.getName());

	private static final long ALARM_MS = INTERVAL_FIFTEEN_MINUTES;

	private final Application app;
	private final AndroidWakeLockManager wakeLockManager;
	private final ScheduledExecutorService scheduledExecutorService;
//	private final AlarmManager alarmManager;

	private final Object lock = new Object();
	private final Queue<ScheduledTask> tasks = new PriorityQueue<>();

	public AndroidTaskScheduler(Application app,
			AndroidWakeLockManager wakeLockManager,
			ScheduledExecutorService scheduledExecutorService) {
		this.app = app;
		this.wakeLockManager = wakeLockManager;
		this.scheduledExecutorService = scheduledExecutorService;
//		alarmManager = (AlarmManager)
//				requireNonNull(app.getSystemService(ALARM_SERVICE));
	}

	@Override
	public void startService() {
//		scheduleAlarm();
	}

	@Override
	public void stopService() {
//		cancelAlarm();
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

//	@Override
//	public void onAlarm(Intent intent) {
//		wakeLockManager.runWakefully(() -> {
//			int extraPid = intent.getIntExtra(EXTRA_PID, -1);
//			int currentPid = Process.myPid();
//			if (extraPid == currentPid) {
//				LOG.info("Alarm");
//				rescheduleAlarm();
//				runDueTasks();
//			} else if (LOG.isLoggable(INFO)) {
//				LOG.info("Ignoring alarm with PID " + extraPid
//						+ ", current PID is " + currentPid);
//			}
//		}, "TaskAlarm");
//	}

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

//	private void scheduleAlarm() {
//		if (SDK_INT >= 23) scheduleIdleAlarm();
//		else scheduleInexactRepeatingAlarm();
//	}
//
//	private void rescheduleAlarm() {
//		// If SDK_INT < 23 the alarm repeats automatically
//		if (SDK_INT >= 23) scheduleIdleAlarm();
//	}

//	private void cancelAlarm() {
//		alarmManager.cancel(getAlarmPendingIntent());
//	}
//
//	private void scheduleInexactRepeatingAlarm() {
//		alarmManager.setInexactRepeating(ELAPSED_REALTIME_WAKEUP,
//				SystemClock.elapsedRealtime() + ALARM_MS, ALARM_MS,
//				getAlarmPendingIntent());
//	}

//	@TargetApi(23)
//	private void scheduleIdleAlarm() {
//		alarmManager.setAndAllowWhileIdle(ELAPSED_REALTIME_WAKEUP,
//				SystemClock.elapsedRealtime() + ALARM_MS,
//				getAlarmPendingIntent());
//	}
//
//	private PendingIntent getAlarmPendingIntent() {
//		Intent i = new Intent(app, AlarmReceiver.class);
//		i.putExtra(EXTRA_PID, Process.myPid());
//		return PendingIntent.getBroadcast(app, REQUEST_ALARM, i,
//				FLAG_CANCEL_CURRENT);
//	}

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
