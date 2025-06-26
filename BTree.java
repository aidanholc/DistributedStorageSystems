import java.io.File;
import java.io.RandomAccessFile;
import java.io.IOException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.List;

public class BTree {
    private static final int maxKeyCount = 6;
   // private static String dbPath = "my_db.btree";
    private static final int pageSize = 4096;
    private int rootPtr;
    private int numPages;
    private RandomAccessFile fileHandle; 

    public BTree(String dbPath) {
        File file = new File(dbPath);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            this.rootPtr = 1;
            this.numPages = 1;

            try {
                this.fileHandle = new RandomAccessFile(file, "rw");
                Node root = new Node(this.rootPtr);
                writeNode(root, true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                this.fileHandle = new RandomAccessFile(file, "rw");
                this.fileHandle.seek(0);
                this.rootPtr = this.fileHandle.readInt();
                this.numPages = this.fileHandle.readInt();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void close() {
        if (fileHandle != null) {
            try {
                fileHandle.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public int get(int key) {
        Node targetNode = findNode(key);
        if (targetNode.hasKey(key)) {
            return targetNode.getValue(key);
        }
        return -1;
    }

    public int insert(int key, int value) {
        Node targetNode = findNode(key);
        if (targetNode.hasKey(key)) {
            int oldValue = targetNode.updateValue(key, value);
            writeNode(targetNode);
            return oldValue;
        }
        if (targetNode.isLeaf() == false) {
            throw new IllegalStateException("First insert isn't on a leaf node"); // Sanity check
        }
        insertLeaf(targetNode, key, value);
        return -1;
    }

    private void insertLeaf(Node x, int key, int value) {
        x.insert(key, value);
        if (x.getKeyCount() > maxKeyCount) {
            splitNode(x);
        } else {
            writeNode(x);
        }
    }

    private void insertNonLeaf(Node x, int key, int value, int childPtr) {
     //   System.out.println("Inserting non leaf");
     //   System.out.println("x: " + x + " key: " + key + " value: " + value + " childPtr: " + childPtr);
        x.insert(key, value, childPtr);
        if (x.getKeyCount() > maxKeyCount) {
            splitNode(x);
        } else {
            writeNode(x);
        }
    }

    private void splitNode(Node x) {
        Object[] splitResult = x.split();
        Node newNode = (Node) splitResult[0];
        newNode.setParent(x.getParent());
        int newNodePtr = getNextPtr();
        newNode.setPtr(newNodePtr); 
        for (int childPtr: newNode.getChildren()) {
            if (childPtr != 0 ) {
                Node child = loadNode(childPtr);
                child.setParent(newNode.getPtr());
                writeNode(child);
            }
        }

        int midKey = (int) splitResult[1];
        int midValue = (int) splitResult[2];
        
        if (x.isRoot()) {
            x.setRoot(false);
            int[] keys = {midKey};
            int[] values = {midValue};
            int[] children = {x.getPtr(), newNode.getPtr()};
            Node root = new Node(keys, values, children, 1, false, true, -1);
            root.setPtr(getNextPtr());
            x.setParent(root.getPtr());
            newNode.setParent(root.getPtr());

            writeNode(root, true);
            writeNode(x);
            writeNode(newNode);
        } else {         
            writeNode(newNode);
            writeNode(x);
            
            Node parent = loadNode(x.getParent());
            insertNonLeaf(parent, midKey, midValue, newNodePtr);
        }
    }

    public Node findNode(int key) {
        Node current = loadNode(rootPtr);
        boolean hasKey = current.hasKey(key);
        while (!hasKey && !current.isLeaf()) {
            current = loadNode(current.getChild(key));
            hasKey = current.hasKey(key);
        }
        return current;
    }


    private Node loadNode(int pagePtr) {
        byte[] buffer = new byte[pageSize];
        try {
            fileHandle.seek(pagePtr * pageSize);
            fileHandle.read(buffer, 0, pageSize);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Node node = new Node(buffer, pagePtr);
        return node;
    }   

    private boolean writeNode(Node node, boolean isRoot) {
        try {
            fileHandle.seek(node.getPtr() * pageSize);
            fileHandle.write(node.serialize());

            if (isRoot) {
                this.rootPtr = node.getPtr();
                fileHandle.seek(0);
                fileHandle.writeInt(node.getPtr());
                fileHandle.writeInt(this.numPages);
            }
            else{
                fileHandle.seek(4);
                fileHandle.writeInt(this.numPages);
            }
            fileHandle.getFD().sync();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
  

    private boolean writeNode(Node node) {
        return writeNode(node, false);
    }

    private int getNextPtr() {
        return ++this.numPages;
    }
    
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        appendToString(result, loadNode(this.rootPtr), 0);
        return "B-Tree Structure:\n" + result.toString();
    }

    private void appendToString(StringBuilder result, Node node, int depth) {
        // Add indentation based on depth
        String indent = "  ".repeat(depth);
        
        // Add current node
        result.append(indent).append("Node (ptr=").append(node.getPtr()).append("): ");
        result.append("keys=").append(Arrays.toString(Arrays.copyOf(node.getKeys(), node.getKeyCount())));
        result.append(", values=").append(Arrays.toString(Arrays.copyOf(node.getValues(), node.getKeyCount())));
        if (!node.isLeaf()) {
            result.append(", children=").append(Arrays.toString(Arrays.copyOf(node.getChildren(), node.getKeyCount() + 1)));
        }
        result.append(", isLeaf=").append(node.isLeaf());
        result.append(", isRoot=").append(node.isRoot());
        result.append("\n");
        
        // Recursively add children if not a leaf
        if (!node.isLeaf()) {
            for (int childPtr : node.getChildren()) {
                if (childPtr != 0) {
                    Node child = loadNode(childPtr);
                    appendToString(result, child, depth + 1);
                }
            }
        }
    }

    public int[] getSortedKeys() {
        ArrayList<Integer> keys = new ArrayList<>();
        getKeys(loadNode(rootPtr), keys);
        return keys.stream().mapToInt(i -> i).toArray();
    }

    private void getKeys(Node node, ArrayList<Integer> keys) {
        if (node.isLeaf()) {
            // For leaf nodes, add all keys in order
            for (int i = 0; i < node.getKeyCount(); i++) {
                keys.add(node.getKeys()[i]);
            }
        } else {
            // For non-leaf nodes, do proper inorder traversal
            for (int i = 0; i <= node.getKeyCount(); i++) {
                // Traverse left child first
                if (node.getChildren()[i] != 0) {
                    getKeys(loadNode(node.getChildren()[i]), keys);
                }
                // Add the key after traversing the left child (except for the last iteration)
                if (i < node.getKeyCount()) {
                    keys.add(node.getKeys()[i]);
                }
            }
        }
    }
} 