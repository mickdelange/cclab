package com.cclab.core.redundancy;

import com.cclab.core.MasterInstance;
import com.cclab.core.data.Database;
import com.cclab.core.network.Message;
import com.cclab.core.utils.NodeLogger;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created on 11/8/14 for CCLabCore.
 *
 * @author an3m0na
 */
public class DataReplicator {

    private ConcurrentLinkedQueue<String> waiting;
    private ConcurrentLinkedQueue<String> stored;
    private MasterInstance master = null;

    private Set<String> backedUnprocessed = null;
    private Set<String> backedProcessed = null;

    public DataReplicator(MasterInstance master) {
        waiting = new ConcurrentLinkedQueue<String>();
        stored = new ConcurrentLinkedQueue<String>();
        backedUnprocessed = new HashSet<String>();
        backedProcessed = new HashSet<String>();
        this.master = master;

    }

    public void backupFutureRecord(String recordId) {
        waiting.add(recordId);
    }

    public void backupPendingRecord(String recordId, byte[] record) {
        backup(Message.Type.BACKUPTASK, recordId, record);
    }

    public void backupFinishedRecord(String recordId, byte[] record) {
        backup(Message.Type.BACKUPFIN, recordId, record);
    }

    public void backupStoredRecord(String recordId) {
        stored.add(recordId);
    }

    private void backup(Message.Type type, String recordId, byte[] data) {
        if (type == Message.Type.BACKUPTASK) {
            if (backedUnprocessed.contains(recordId))
                return;
        } else {
            if (backedProcessed.contains(recordId))
                return;
        }

        if (data == null || data.length <= 0) {
            data = Database.getInstance().getRecord(recordId);
            if (data == null || data.length <= 0) {
                NodeLogger.get().error("Record " + recordId + " will not be replicated");
                return;
            }
        }
        waiting.remove(recordId);
        stored.remove(recordId);
        if (type == Message.Type.BACKUPTASK)
            backedUnprocessed.add(recordId);
        else
            backedProcessed.add(recordId);

        Message message = new Message(type, master.myName);
        message.setDetails(recordId);
        message.setData(data);
        master.sendToBackup(message);
    }

    public void doBackup() {
        while (!stored.isEmpty()) {
            String recordId = stored.poll();
            backup(Message.Type.BACKUPFIN, recordId, null);
        }
        while (!waiting.isEmpty()) {
            String recordId = waiting.poll();
            if (stored.contains(recordId))
                continue;
            backup(Message.Type.BACKUPTASK, recordId, null);
        }
    }

    public void backupAll() {
        List<String> storedList = Arrays.asList(Database.getInstance().getStoredRecords());
        stored.addAll(storedList);
        List<String> processingList = Arrays.asList(Database.getInstance().getTemporaryRecords());
        processingList.removeAll(stored);
        waiting.addAll(processingList);
    }
}
