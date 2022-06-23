package com.eugene.wc.work;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.eugene.wc.protocol.api.contact.ContactId;
import com.eugene.wc.protocol.api.contact.ContactManager;
import com.eugene.wc.protocol.api.db.exception.DbException;

import javax.inject.Inject;

public class TransportKeyRotationWork extends Worker {

    public static final String TAG = TransportKeyRotationWork.class.getName();

    public static final String WORK_TAG = "transport_key_rotation";
    public static final String CONTACT_ID_KEY = "CONTACT_ID";

    private final ContactManager contactManager;

    public TransportKeyRotationWork(@NonNull Context context,
                                    @NonNull WorkerParameters workerParams,
                                    ContactManager contactManager) {
        super(context, workerParams);
        this.contactManager = contactManager;
    }

    @NonNull
    @Override
    public Result doWork() {
        Data inputData = getInputData();
        int id = inputData.getInt(CONTACT_ID_KEY, -1);
        if (id == -1) {
            return Result.failure();
        }
        ContactId contactId = new ContactId(id);
        boolean rotated = false;
        try {
            contactManager.rotateContactKeys(contactId);
            rotated = true;
        } catch (DbException e) {
            Log.w(TAG, "Unable to rotate contact keys", e);
        }

        return rotated ? Result.success() : Result.retry();
    }

    static class Factory implements ChildWorkerFactory {

        private final ContactManager contactManager;

        @Inject
        public Factory(ContactManager contactManager) {
            this.contactManager = contactManager;
        }

        @Override
        public Worker createWorker(Context context, WorkerParameters workerParams) {
            return new TransportKeyRotationWork(context, workerParams, contactManager);
        }
    }
}
