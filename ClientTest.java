// // Source code is decompiled from a .class file using FernFlower decompiler.
// import static org.junit.Assert.assertEquals;

// import java.util.Arrays;
// import org.junit.Assert;
// import org.junit.Test;

// public class ClientTest {
//     private final int port = 5460;
//     public ClientTest() {
//     }

//     @Test
//     public void testTransactionRead() {
//         // Server is connected to a database with keys 0-39,999
//         Client client = new Client(port);
//         long startTime = System.currentTimeMillis();
//         String response = client.sendMessage("BEGIN 1 2 3");
//         System.out.println(response);
//         long expiration_time = Long.getLong(response);
//         Assert.assertTrue(expiration_time > startTime);

//         response = client.sendMessage("READ 2");
//         int int_response = Integer.getInteger(response);
//         Assert.assertEquals(200, int_response);
//     }

//     @Test
//     public void testNodeInsertion() {
//         Node var1 = new Node(1);
//         var1.insert(1, 100);
//         var1.insert(2, 200);
//         var1.insert(3, 300);
//         System.out.println("Keys: " + Arrays.toString(var1.getKeys()));
//         System.out.println("Values: " + Arrays.toString(var1.getValues()));
//         Assert.assertArrayEquals(var1.getKeys(), new int[]{1, 2, 3, 0, 0, 0, 0});
//         Assert.assertArrayEquals(var1.getValues(), new int[]{100, 200, 300, 0, 0, 0, 0});
//     }

//     @Test
//     public void testNodeSplit() {
//         Node var1 = new Node(1);
//         var1.insert(1, 100);
//         var1.insert(2, 200);
//         var1.insert(3, 300);
//         var1.insert(4, 400);
//         var1.insert(5, 500);
//         var1.insert(6, 600);
//         var1.insert(7, 700);
//         Object[] var2 = var1.split();
//         Node var3 = (Node)var2[0];
//         int var4 = (Integer)var2[1];
//         int var5 = (Integer)var2[2];
//         Assert.assertEquals((long)var4, 4L);
//         Assert.assertEquals((long)var5, 400L);
//         System.out.println("Keys: " + Arrays.toString(var3.getKeys()));
//         System.out.println("Values: " + Arrays.toString(var3.getValues()));
//         Assert.assertArrayEquals(var1.getKeys(), new int[]{1, 2, 3, 0, 0, 0, 0});
//         Assert.assertArrayEquals(var3.getKeys(), new int[]{5, 6, 7, 0, 0, 0, 0});
//         Assert.assertArrayEquals(var1.getValues(), new int[]{100, 200, 300, 0, 0, 0, 0});
//         Assert.assertArrayEquals(var3.getValues(), new int[]{500, 600, 700, 0, 0, 0, 0});
//     }
// }

