import static org.junit.Assert.*;
import org.junit.Test;
import java.util.Arrays;

public class NodeTest {
    @Test
    public void testNodeSerialization() {
        Node node = new Node(new int[] {1, 12, 30}, new int[] {100, 200, 300}, new int[] {0, 10, 14, 40}, 3, true, true, 1);
        byte[] serialized = node.serialize();
        Node deserialized = new Node(serialized, 0);
        System.out.println("Keys: " + Arrays.toString(node.getKeys()));
        System.out.println("deserialized: " + Arrays.toString(deserialized.getKeys()));
        assertArrayEquals(node.getKeys(), deserialized.getKeys());
        assertArrayEquals(node.getValues(), deserialized.getValues());
        assertArrayEquals(node.getChildren(), deserialized.getChildren());
    }

    @Test
    public void testNodeInsertion() {
        Node node = new Node(1);
        node.insert(1, 100);
        node.insert(2, 200);
        node.insert(3, 300);
        System.out.println("Keys: " + Arrays.toString(node.getKeys()));
        System.out.println("Values: " + Arrays.toString(node.getValues()));
        assertArrayEquals(node.getKeys(), new int[] {1, 2, 3, 0, 0, 0, 0});
        assertArrayEquals(node.getValues(), new int[] {100, 200, 300, 0, 0, 0, 0});
    }
    @Test
    public void testNodeSplit() {
        Node node = new Node(1);
        node.insert(1, 100);
        node.insert(2, 200);
        node.insert(3, 300);
        node.insert(4, 400);
        node.insert(5, 500);
        node.insert(6, 600);
        node.insert(7, 700);
        Object[] splitResult = node.split();
        Node newNode = (Node) splitResult[0];
        int midKey = (int) splitResult[1];
        int midValue = (int) splitResult[2];
        assertEquals(midKey, 4);
        assertEquals(midValue, 400);
        System.out.println("Keys: " + Arrays.toString(newNode.getKeys()));
        System.out.println("Values: " + Arrays.toString(newNode.getValues()));
        assertArrayEquals(node.getKeys(), new int[] {1, 2, 3, 0, 0, 0, 0});
        assertArrayEquals(newNode.getKeys(), new int[] {5, 6, 7, 0, 0, 0, 0});
        assertArrayEquals(node.getValues(), new int[] {100, 200, 300, 0, 0, 0, 0});
        assertArrayEquals(newNode.getValues(), new int[] {500, 600, 700, 0, 0, 0, 0});
    }
}
