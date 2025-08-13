import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;

public class ServerAdapter {
    private WAL wal;
    private BTree data;
    private HashMap<Integer, HashMap<Integer, Integer>> buffers;
    private TreeSet<Integer> lockedKeys;
    private HashMap<Integer, ArrayList<Integer>> transactionLockedKeys;

    public ServerAdapter(String BTreePath, String WALPath)  {
        this.data = new BTree(BTreePath);
        this.wal = new WAL(WALPath);
        this.buffers = new HashMap<>();
        this.lockedKeys = new TreeSet<>();
        this.transactionLockedKeys = new HashMap<>();
    }

    public boolean lockKeys(int transactionID, int[] keys) {
        for (int key: keys) {
            if (this.lockedKeys.contains(key)) {
                return false;
            }
        }
        for (int key: keys) {
            this.lockedKeys.add(key);
        }
        ArrayList<Integer> transactionKeys = new ArrayList<>(keys.length);
        for (int key: keys) {
            transactionKeys.add(key);
        }
        this.transactionLockedKeys.put(transactionID, transactionKeys);
        this.wal.transaction(transactionID);
        return true;
    }

    public Integer put(int transactionID, int key, int value) {
        return this.put(transactionID, key, value, true);
    }

    public Integer put(int transactionID, int key, int value, boolean isTransaction) {
        if (!isTransaction && this.lockedKeys.contains(key)) {
            throw new IllegalAccessError("Trying to alter a locked key"); 
        }

        HashMap<Integer, Integer> buffer = getBuffer(transactionID);
        Integer return_val = buffer.put(key, value);
        if (return_val == null) {
            return_val = this.data.get(key);
            if (return_val == -1) {
                return_val = null; // Key not found
            }
        }
        if (!isTransaction) {
            this.wal.transaction(transactionID); // Write transaction info to the wal if this write is not part of a transaction
            this.commit(transactionID); // Commit the write immediately
        } 
        this.wal.write(transactionID, key, value, return_val);
        return return_val;
    }

    public Integer get(int transactionID, int key) {
        return this.get(transactionID, key, true);
    }

    public Integer get(int transactionID, int key, boolean isTransaction) {
        if (!isTransaction && this.lockedKeys.contains(key)) {
            throw new IllegalAccessError("trying to access a locked key");
        }
        HashMap<Integer, Integer> buffer = getBuffer(transactionID);
        Integer return_val = buffer.get(key);
        if (return_val == null) {
            return_val = Integer.valueOf(this.data.get(key));
            if (return_val == -1) {
                return_val = null; // Key not found
            }
        } 
        return return_val;
    }

    public boolean contains(int transactionID, int key) {
        HashMap<Integer, Integer> buffer = getBuffer(transactionID);
        boolean return_val = buffer.containsKey(key);
        return return_val;
    }

    public boolean commit(int transactionID) {
        HashMap<Integer, Integer> transaction_buffer = getBuffer(transactionID); 
        try {
            for (Integer key : transaction_buffer.keySet()) {
                this.data.insert(key, transaction_buffer.get(key));
            }
            for (Integer key: this.transactionLockedKeys.get(transactionID)) {
                this.lockedKeys.remove(key);
            }
            this.buffers.remove(transactionID);
            this.transactionLockedKeys.remove(transactionID);
            this.wal.commit(transactionID);
        } catch (Exception e) {
            System.err.println("Commit failed: " + e.getMessage());
            return false;
        }
        return true;
    }

    public void abort(int transactionID) {
        if (!this.buffers.containsKey(transactionID)) {
            return; // Transaction not found
        }
        for (Integer key: this.transactionLockedKeys.get(transactionID)) {
            this.lockedKeys.remove(key);
        }
        this.buffers.remove(transactionID);
        this.transactionLockedKeys.remove(transactionID);
        this.wal.abort(transactionID);
    }

    public boolean initializeTransaction(int transactionID) {
        if (this.buffers.get(transactionID) == null) {
            this.buffers.put(transactionID, new HashMap<>());
            this.transactionLockedKeys.put(transactionID, new ArrayList<>());
            return true;
        }
        return false; // Transaction already initialized
    }

    public boolean hasChanges(int transactionID) {
        HashMap<Integer, Integer> transaction_buffer = getBuffer(transactionID);
        return !transaction_buffer.isEmpty();
    }


    private HashMap<Integer, Integer> getBuffer(int transactionID) {
        if (!this.buffers.containsKey(transactionID)) {
            throw new IllegalArgumentException("Transaction ID not found");
        }
        HashMap<Integer, Integer> transaction_buffer = this.buffers.get(transactionID);
        return transaction_buffer;
    }

}
