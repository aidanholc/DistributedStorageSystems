import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Before;
import java.util.Arrays;
import java.io.File;
import java.util.ArrayList;

public class BTreeTest {
    
    @Before
    public void setUp() {
        // Clean up the database file before each test
        File dbFile = new File("my_db.btree");
        if (dbFile.exists()) {
            dbFile.delete();
        }
    }
    
    @Test
    public void testBTreeCreation() {
        BTree btree = new BTree("test_db.btree");
        assertEquals(btree.get(1), -1);
    }

    @Test
    public void testBTreeFindNode() {
        BTree btree = new BTree("test_db.btree");
        btree.insert(1, 100);
        btree.insert(2, 200);
        btree.insert(3, 300);
        btree.insert(4, 400);
        assertArrayEquals(btree.findNode(1).getKeys(), new int[] {1, 2, 3, 4, 0, 0, 0});
    }

    @Test
    public void testBTreeSplit() {
        BTree btree = new BTree("test_db.btree");
        btree.insert(1, 100);
        btree.insert(2, 200);
        btree.insert(3, 300);
        btree.insert(4, 400);
        btree.insert(5, 500);
        btree.insert(6, 600);
        btree.insert(7, 700);
        assertArrayEquals(btree.findNode(1).getKeys(), new int[] {1, 2, 3, 0, 0, 0, 0});
        assertArrayEquals(btree.findNode(5).getKeys(), new int[] {5, 6, 7, 0, 0, 0, 0});
        assertArrayEquals(btree.findNode(4).getKeys(), new int[] {4, 0, 0, 0, 0, 0, 0});
    }

    @Test
    public void testBTreeGetSortedKeys() {
        BTree btree = new BTree("test_db.btree");
        int numKeys = 9000;
        ArrayList<Integer> expectedKeys = new ArrayList<>();
        for (int i = 0; i < numKeys; i++) {
            btree.insert(i, i * 100);
            expectedKeys.add(i);
        }
        int[] sortedKeys = btree.getSortedKeys();    
        assertArrayEquals(sortedKeys, expectedKeys.stream().mapToInt(i -> i).toArray());
    }

    @Test
    public void testBTreeInsertionLarge() {
        BTree btree = new BTree("test_db.btree");
        int numKeys = 40000;
        for (int i = 0; i < numKeys; i++) {
            btree.insert(i, i * 100);
        }
        System.out.println(btree);
        System.out.println(btree.findNode(25));

        for (int i = 0; i < numKeys; i++) {
            assertEquals(btree.get(i), i * 100);
        }
    }

    @Test
    public void testBTreeInsertion() {
        BTree btree = new BTree("test_db.btree");
        btree.insert(1, 100);
        btree.insert(2, 200);
        btree.insert(3, 300);
        assertEquals(btree.get(1), 100);
        assertEquals(btree.get(2), 200);
        assertEquals(btree.get(3), 300);
        assertEquals(btree.get(4), -1);
    }
}