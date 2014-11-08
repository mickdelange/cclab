package com.cclab.core.redundancy;

import com.cclab.core.MasterInstance;
import com.cclab.core.data.Database;
import com.cclab.core.network.Message;
import com.cclab.core.utils.NodeLogger;

import java.util.Arrays;
import java.util.List;
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

    public DataReplicator(MasterInstance master) {
        waiting = new ConcurrentLinkedQueue<String>();
        stored = new ConcurrentLinkedQueue<String>();
        this.master = master;
    }

    public void backupFutureRecord(String recordId) {
        if (!waiting.contains(recordId))
            waiting.add(recordId);
    }

    public void backupPendingRecord(String recordId, byte[] record) {
        waiting.remove(recordId);
        stored.remove(recordId);
        backup(Message.Type.BACKUPTASK, recordId, record);
    }

    public void backupFinishedRecord(String recordId, byte[] record) {
        waiting.remove(recordId);
        stored.remove(recordId);
        backup(Message.Type.BACKUPFIN, recordId, record);
    }

    public void backupStoredRecord(String recordId) {
        //remove from waiting if already processed
        waiting.remove(recordId);
        if (!stored.contains(recordId))
            stored.add(recordId);
    }

    private void backup(Message.Type type, String recordId, byte[] data) {

        if (data == null || data.length <= 0) {
            data = Database.getInstance().getRecord(recordId);
            if (data == null || data.length <= 0) {
                NodeLogger.get().error("Record " + recordId + " will not be replicated");
                return;
            }
        }
        waiting.remove(recordId);
        stored.remove(recordId);

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
