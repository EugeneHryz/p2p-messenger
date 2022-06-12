package com.eugene.wc.work;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.work.ListenableWorker;
import androidx.work.WorkerParameters;

import java.util.Map;

import javax.inject.Inject;

public class WorkerFactory extends androidx.work.WorkerFactory {

    private final Map<Class<?>, ChildWorkerFactory> workerFactories;

    @Inject
    public WorkerFactory(Map<Class<?>, ChildWorkerFactory> workerFactories) {
        this.workerFactories = workerFactories;
    }

    @Nullable
    @Override
    public ListenableWorker createWorker(@NonNull Context appContext, @NonNull String workerClassName,
                                         @NonNull WorkerParameters workerParameters) {
        try {
            ChildWorkerFactory factory = workerFactories.get(Class.forName(workerClassName));

            if (factory != null) {
                return factory.createWorker(appContext, workerParameters);
            }
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(e);
        }
        throw new IllegalArgumentException("Unable to find ChildWorkerFactory");
    }
}
