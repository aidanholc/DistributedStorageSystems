/*
 *  Structure of a page:
 *  byte 0 - isLeaf
 *  byte 1 - isRoot  
 *  bytes 2-5 parent offset
 *  bytes 6-9 num of active keys
 *  bytes 10-13 child 0 offset
 *  bytes 14-17 key 0
 *  bytes 18-21 value 0
 *  bytes 22-25 child 1 offset
 *  bytes 26-29 key 1
 *  bytes 30-33 value 1
 *  bytes 34-37 child 2 offset
 *  bytes 38-41 key 2
 *  bytes 42-45 value 2
 *  bytes 46-49 child 3 offset
 *  bytes 50-53 key 3
 *  bytes 54-57 value 3
 *  bytes 58-61 child 4 offset
 *  bytes 62-65 key 4
 *  bytes 66-69 value 4
 *  bytes 70-73 child 5 offset
 *  bytes 74-77 key 5
 *  bytes 78-81 value 5
 *  bytes 82-85 child 6 offset
 *  bytes 86-89 key 6
 *  bytes 90-93 value 6
 *  bytes 94-97 child 7 offset
 */

import java.nio.ByteBuffer;
import java.util.Arrays;

public class Node {
    private static final int MAX_KEY_VALUES = 6;
    private static final int MAX_CHILDREN = 7;
    private static final int pageSize = 4096;

    private int[] keys;
    protected int[] children; 
    private int[] values;
    private int key_count;
    private boolean isLeaf;
    private boolean isRoot;
    private int parent_offset;
    private int thisPtr;

    public Node(int thisPtr) { 
        this.keys = new int[MAX_KEY_VALUES + 1];
        this.values = new int[MAX_KEY_VALUES + 1];
        this.children = new int[MAX_CHILDREN + 1];
        this.key_count = 0;
        this.isLeaf = true;
        this.isRoot = true;
        this.parent_offset = -1;
        this.thisPtr = thisPtr;
    }

    public Node(byte[] byteArray, int thisPtr) {
        this.keys = new int[MAX_KEY_VALUES + 1];
        this.values = new int[MAX_KEY_VALUES + 1];
        this.children = new int[MAX_CHILDREN + 1];

        ByteBuffer buffer = ByteBuffer.wrap(byteArray);
        this.isLeaf = buffer.get() == 1;
        this.isRoot = buffer.get() == 1;
        this.parent_offset = buffer.getInt();
        this.key_count = buffer.getInt();
        this.thisPtr = thisPtr;
        for (int i = 0; i < this.key_count; i++) {
            this.children[i] = buffer.getInt();
            this.keys[i] = buffer.getInt();
            this.values[i] = buffer.getInt();
        }
        this.children[this.key_count] = buffer.getInt();

        }

    public Node(int[] keys, int[] values, int[] children, int key_count, boolean isLeaf, boolean isRoot, int parent_offset) {
        this.keys = new int[MAX_KEY_VALUES + 1];
        this.values = new int[MAX_KEY_VALUES + 1];
        this.children = new int[MAX_CHILDREN + 1];
        this.key_count = key_count;
        this.isLeaf = isLeaf;
        this.isRoot = isRoot;
        this.parent_offset = parent_offset;
        this.thisPtr = -1;

        System.arraycopy(values, 0, this.values, 0, values.length);
        System.arraycopy(keys, 0, this.keys, 0, keys.length);
        System.arraycopy(children, 0, this.children, 0, children.length);
    }

    public boolean isLeaf() {
        return this.isLeaf;
    }

    public boolean isRoot() {
        return this.isRoot;
    }

    public void setRoot(boolean isRoot) {
        this.isRoot = isRoot;
    }

    public int getPtr() {
        return this.thisPtr;
    }

    public void setPtr(int thisPtr) {
        this.thisPtr = thisPtr;
    }

    public int getParent() {
        return this.parent_offset;
    }

    public void setParent(int parent_offset) {
        this.parent_offset = parent_offset;
    }

    public int getKeyCount() {
        return this.key_count;
    }

    public boolean hasKey(int key) {
        for (int i = 0; i < this.key_count; i++) {
            if (this.keys[i] == key) {
                return true;
            }
        }
        return false;
    }

    public int getValue(int key) {
        for (int i = 0; i < this.key_count; i++) {
            if (this.keys[i] == key) {
                return this.values[i];
            }
        }
        return -1;

    }

    public int getChild(int key) { 
        for (int i = 0; i < this.key_count; i++) {
            if (this.keys[i] > key)  return this.children[i];// return the child left of the key
        }
        return this.children[this.key_count]; // return the child to the right of the last key if no key is greater than the given key
    }

    public void setChild(int idx, int childPtr) {
        this.children[idx] = childPtr;
    }

    public int updateValue(int key, int value) { 
        for (int i = 0; i < this.key_count; i++) {
            if (this.keys[i] == key) {
                int oldValue = this.values[i];
                this.values[i] = value;
                return oldValue;
            } else if (this.keys[i] > key) {
                throw new IllegalStateException("Trying to update a key that doesn't exist");
            }
        }
        throw new IllegalStateException("Trying to update a key that doesn't exist");
    }

    public int insert(int key, int value, int childPtr) {
        /**
         * Insert a key-value pair into a non-leaf node.
         * 
         * @param key
         * @param value
         * @return the index of the inserted key
         */
        int i = this.key_count - 1;
        for (; i >= 0; i--) {
            if (this.keys[i] > key) {
                this.keys[i + 1] = this.keys[i];
                this.values[i + 1] = this.values[i];
                this.children[i + 2] = this.children[i + 1];
            } else {
               break;
            }
        }
        this.keys[i+1] = key;
        this.values[i+1] = value;   
        this.children[i+2] = childPtr;
        this.key_count++;
       // System.out.println(this);
        return i;
    }

    public int insert(int key, int value) { 
        /**
         * Insert a key-value pair into a leaf node.
         * 
         * @param key
         * @param value
         * @return the index of the inserted key
         */
        if (!this.isLeaf()) {
            throw new IllegalStateException("Must specify childPtr when inserting into a non-leaf node");
        }
        int i = this.key_count - 1;
        for (; i >= 0; i--) {
            if (this.keys[i] > key) {
                this.keys[i + 1] = this.keys[i];
                this.values[i + 1] = this.values[i];
                this.children[i + 2] = this.children[i + 1];
            } else {
               break;
            }
        }
        this.keys[i+1] = key;
        this.values[i+1] = value;
        this.children[i+2] = 0;
        this.key_count++;
        return i;
    }


    public Object[] split() {
        int mid = (this.key_count / 2); // always round up
        int[] newKeys = Arrays.copyOfRange(this.keys, mid + 1, this.key_count);
        int[] newValues = Arrays.copyOfRange(this.values, mid + 1, this.key_count);
        
        int[] newChildren;
        if (this.isLeaf) {
            // For leaf nodes, children array is not used meaningfully
            newChildren = new int[MAX_CHILDREN + 1];
        } else {
            newChildren = new int[MAX_CHILDREN + 1];
            System.arraycopy(this.children, mid + 1, newChildren, 0, this.key_count - mid);
        }
        
        // Don't set parent offset here - let BTree handle it
        Node newNode = new Node(newKeys, newValues, newChildren, newKeys.length, this.isLeaf, false, -1);
        int midKey = this.keys[mid];
        int midValue = this.values[mid];
        
        // Update the original node's arrays
        int[] thisNewKeys = new int[MAX_KEY_VALUES + 1];
        System.arraycopy(Arrays.copyOfRange(this.keys, 0, mid), 0, thisNewKeys, 0, mid);
        this.keys = thisNewKeys;

        int[] thisNewValues = new int[MAX_KEY_VALUES + 1];
        System.arraycopy(Arrays.copyOfRange(this.values, 0, mid), 0, thisNewValues, 0, mid);
        this.values = thisNewValues;

        int[] thisNewChildren = new int[MAX_CHILDREN + 1];
        System.arraycopy(Arrays.copyOfRange(this.children, 0, mid + 1), 0, thisNewChildren, 0, mid + 1);
        this.children = thisNewChildren;

        this.key_count = mid;

        return new Object[] {newNode, midKey, midValue};   
    }

    public byte[] serialize() {
        ByteBuffer buffer = ByteBuffer.allocate(pageSize);
        buffer.put(this.isLeaf ? (byte) 1 : (byte) 0);
        buffer.put(this.isRoot ? (byte) 1 : (byte) 0);
        buffer.putInt(this.parent_offset);
        buffer.putInt(this.key_count);
        for (int i = 0; i < this.key_count; i++) {
            buffer.putInt(this.children[i]);
            buffer.putInt(this.keys[i]);
            buffer.putInt(this.values[i]);
        }
        buffer.putInt(this.children[this.key_count]);
        return buffer.array();
    }

    public void incrementKeyCount() {
        this.key_count++;
    }

    public void decrementKeyCount() {
        this.key_count--;
    }

    public int[] getKeys() { 
        return this.keys;
    }

    public int[] getValues() { 
        return this.values;
    }
    
    public int[] getChildren() { 
        return this.children;
    }

    @Override
    public String toString() {
        return "Node(\n\tkeys=" + Arrays.toString(this.keys) + "\n\tvalues=" + Arrays.toString(this.values) + "\n\tchildren=" + Arrays.toString(this.children) + "\n\tkey_count=" + this.key_count + "\n\tisLeaf=" + this.isLeaf + "\n\tisRoot=" + this.isRoot + "\n\tparent_offset=" + this.parent_offset + "\n\tthisPtr=" + this.thisPtr + ")";
    }
}
