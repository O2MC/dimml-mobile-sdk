package io.o2mc.sdk.business.batch;

import io.o2mc.sdk.domain.Batch;
import io.o2mc.sdk.domain.DeviceInformation;
import io.o2mc.sdk.domain.Event;
import io.o2mc.sdk.domain.Operation;
import io.o2mc.sdk.exceptions.O2MCInternalException;
import io.o2mc.sdk.interfaces.O2MCExceptionNotifier;
import io.o2mc.sdk.util.TimeUtil;
import java.util.ArrayList;
import java.util.List;

import static io.o2mc.sdk.util.LogUtil.LogD;

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

  private O2MCExceptionNotifier o2mcExceptionNotifier;

  BatchBus(O2MCExceptionNotifier o2mcExceptionNotifier) {
    this.o2mcExceptionNotifier = o2mcExceptionNotifier;
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

  public Batch generateBatch(String batchId, List<Event> events, List<Operation> operations) {
    // Events may be null/empty
    // Operations may be null/empty
    // But at least one of those lists must be non-empty

    // Make sure we have a valid list of events
    List<Event> eventsLocal;
    if (events == null) {
      eventsLocal = new ArrayList<>();
    } else if (events.size() == 0) {
      eventsLocal = new ArrayList<>();
    } else { // We have at least one event, use it
      eventsLocal = new ArrayList<>(events);
    }

    // Make sure we have a valid list of operations
    List<Operation> operationsLocal;
    if (operations == null) {
      operationsLocal = new ArrayList<>();
    } else if (operations.size() == 0) {
      operationsLocal = new ArrayList<>();
    } else { // We have at least one operation, use it
      operationsLocal = new ArrayList<>(operations);
    }

    if (eventsLocal.size() == 0 && operationsLocal.size() == 0) {
      // If we have neither any events nor operations, we shouldn't even be creating this batch
      // Something went wrong earlier, notify developer
      o2mcExceptionNotifier.notifyException(
          new O2MCInternalException(
              "There are no events nor operations while generating a batch. We should not be generating a batch without either of those. Find out how we got to this point and prevent it from happening again."),
          false
          // Not fatal, just unusual behavior which should be looked into before it causes any trouble in the future
      );
    }

    return new Batch(
        deviceInformation,
        TimeUtil.generateTimestamp(),
        eventsLocal,
        operationsLocal,
        batchCounter++, /*add 1 to the counter after this statement*/
        batchId,
        0
    );
  }

  /**
   * Called when the most recent batch has failed to be processed by the backend.
   */
  public void onBatchFailed() {
    retries++;
    awaitingCallback = false;
    LogD(TAG, String.format("Last batch failed. Retries is '%s' now.", retries));
  }

  /**
   * Called when the most recent batch has successfully been processed by the backend.
   */
  public void onBatchSucceeded() {
    retries = 0;
    pendingBatch = null;
    awaitingCallback = false;
    LogD(TAG, String.format("Last batch succeeded. Retries is '%s' now.", retries));
  }

  public int getRetries() {
    return retries;
  }

  public void add(Batch b) {
    LogD(TAG,
        String.format("Added batch - identified by number '%s' - to BatchBus.", b.getNumber()));

    batches.add(b);
  }

  public void clearBatches() {
    batches.clear();
  }

  public void clearPending() {
    pendingBatch = null;
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
          // There are no batches to set as pending, do nothing.
          break;
        case 1:
          pendingBatch = batches.get(0); // set the only batch as pending
          clearBatches(); // remove from bus to prevent resending it later
          break;
        default:
          pendingBatch = mergeBatches(batches); // set all batches as a new big one on pending
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
   * @return a big(ger) Batch containing the events of every batch currently in the BatchBus, null if no batches provided
   */
  private Batch mergeBatches(List<Batch> batches) {
    if (batches.size() <= 0) return null; // can't merge non existent batches

    List<Event> allEvents = new ArrayList<>();
    for (Batch b : batches) {
      allEvents.addAll(b.getEvents());
    }

    List<Operation> allOperations = new ArrayList<>();
    for (Batch b : batches) {
      allOperations.addAll(b.getOperations());
    }

    // The batch ID is the same for every batch in the current user session, doesn't matter if we get the 1st one or the last one
    String batchId = batches.get(0).getSessionId();

    return generateBatch(batchId, allEvents, allOperations);
  }

  /**
   * Executed before dispatching batch(es) from BatchManager.
   */
  public void preDispatch() {
    awaitingCallback = true;
    pendingBatch.setRetries(retries);
  }
}
