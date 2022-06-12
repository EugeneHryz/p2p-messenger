package com.eugene.wc.work;

import android.content.Context;

import androidx.work.Worker;
import androidx.work.WorkerParameters;

public interface ChildWorkerFactory {

    Worker createWorker(Context context, WorkerParameters workerParams);
}
