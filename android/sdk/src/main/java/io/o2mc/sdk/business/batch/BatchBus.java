package io.o2mc.sdk.business.batch;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import io.o2mc.sdk.BuildConfig;
import io.o2mc.sdk.domain.Batch;
import io.o2mc.sdk.domain.DeviceInformation;
import io.o2mc.sdk.domain.Event;
import io.o2mc.sdk.util.TimeUtil;

/**
 * Holds all batches generated by clearing the EventBus. Similar to the EventBus, but used for holding Batch objects.
 */
public class BatchBus {

    private static final String TAG = "BatchBus";

    private DeviceInformation deviceInformation;

    private final List<Batch> batches;

    private static int batchCounter = 0; // represents how many batches have been generated so far
    private int retries = 0; // represents the amount of times in a row batches have failed to be sent

    private boolean awaitingCallback;
    private Batch pendingBatch;

    public BatchBus() {
        this.batches = new ArrayList<>();
    }


    /**
     * Sets device information. Only set it if it hasn't been set previously.
     *
     * @param deviceInformation information about this device
     */
    public void setDeviceInformation(DeviceInformation deviceInformation) {
        if (this.deviceInformation == null) {
            this.deviceInformation = deviceInformation;
        }
    }

    public Batch generateBatch(List<Event> events) {
        if (BuildConfig.DEBUG)
            Log.i(TAG, String.format("generateBatch: Generating batch with '%s' events", events.size()));

        return new Batch(
                deviceInformation,
                TimeUtil.generateTimestamp(),
                new ArrayList<>(events), // generate a new list, don't use a reference to the list
                batchCounter++ /*add 1 to the counter after this statement*/
        );
    }

    /**
     * Called when the most recent batch has failed to be processed by the backend.
     */
    public void lastBatchFailed() {
        retries++;
        awaitingCallback = false;

        if (BuildConfig.DEBUG)
            Log.d(TAG, String.format("Last batch failed. Retries is '%s' now.", retries));
    }

    /**
     * Called when the most recent batch has successfully been processed by the backend.
     */
    public void lastBatchSucceeded() {
        retries = 0;
        pendingBatch = null;
        awaitingCallback = false;

        if (BuildConfig.DEBUG)
            Log.d(TAG, String.format("Last batch succeeded. Retries is '%s' now.", retries));
    }

    public int getRetries() {
        return retries;
    }

    public void add(Batch b) {
        if (BuildConfig.DEBUG)
            Log.d(TAG, String.format("Added batch - identified by number '%s' - to BatchBus.", b.getNumber()));

        batches.add(b);
    }

    public void clearBatches() {
        batches.clear();

        if (BuildConfig.DEBUG)
            Log.d(TAG, "clearBatches: Cleared the BatchBus.");
    }

    /**
     * Checks whether or not we're currently awaiting a callback from previous dispatch.
     *
     * @return true if we're still waiting for a http response
     */
    public boolean awaitingCallback() {
        return awaitingCallback;
    }

    /**
     * Sets a new batch as pending.
     * Optimizes / reduces overhead where possible.
     */
    public void setPendingBatch() {
        synchronized (batches) { // another thread may be updating the BatchBus
            switch (batches.size()) {
                case 0:
                    if (BuildConfig.DEBUG)
                        Log.w(TAG, "setPendingBatch: There are no batches to set as pending.");
                    break;
                case 1:
                    if (BuildConfig.DEBUG)
                        Log.d(TAG, "setPendingBatch: There's currently one batch in the BatchBus. Setting it as pending.");
                    pendingBatch = batches.get(0); // set the only batch as pending
                    clearBatches(); // remove from bus to prevent resending it later
                    break;
                default:
                    if (BuildConfig.DEBUG)
                        Log.d(TAG, String.format("setPendingBatch: There are currently more than one ('%s') batches in the BatchBus. Merging.", batches.size()));
                    pendingBatch = mergeBatches(batches); // set all batches in the batch bus as a new big one on pending
                    clearBatches(); // remove from bus to prevent resending it later
                    break;
            }
        }
    }

    public Batch getPendingBatch() {
        return pendingBatch;
    }

    /**
     * Merges all batches currently in the BatchBus.
     * This removes meta data from old batches, and generates a new batch by only keeping all of the events from old batches.
     * This reduces overhead and thus saves data (by sending less meta data in total).
     *
     * @return a big(ger) Batch containing the events of every batch currently in the BatchBus
     */
    private Batch mergeBatches(List<Batch> batches) {
        List<Event> allEvents = new ArrayList<>();
        for (Batch b : batches) {
            allEvents.addAll(b.getEvents());
        }
        return generateBatch(allEvents);
    }

    /**
     * Executed when starting to dispatch batch(es) from BatchManager.
     */
    public void onDispatch() {
        awaitingCallback = true;
        pendingBatch.setRetries(retries);
    }
}
