
public class ClientRunner {
/**
 *  Valid Syntax:
 * 
 *  BEGIN <key1> <key2> ... ;
 *  WRITE <key> <value> ;
 *  READ <key> ;
 *  CONTAINS <key> ;
 *  COMMIT ;
 *  ABORT ;
 *  SHUTDOWN ;
 * 
 *  **/
    private static final int port = 5460;
    public static void main(String[] args) {
        boolean result = testTransactionRead(true);
        System.out.println("testTransactionRead: " + result);

        result = testMultipleClientTransactionRead(true);
        System.out.println("testMultipleClientTransactionRead: " + result);

        result = testBadRequest(false);
        System.out.println("testBadRequest: " + result);
        // BTree oneMillionBTree = new BTree("1mil.btree");
        // for (int i = 0; i < 1_000_000; i++) {
        //     oneMillionBTree.insert(i, i* 100);
        //     if (i % 50_000 == 0) {
        //         System.out.println(i);
        //     }
        // }
    }

    public static boolean testTransactionRead(boolean debug) {
        Client client = new Client(port);
        long startTime = System.currentTimeMillis();
        String response = client.sendMessage("BEGIN 1 2 3 ;", false);
        System.out.println(response);
        long expiration_time = Long.parseLong(response);
        boolean beginResult = expiration_time > startTime;
        if (debug) System.out.println("Begin status: " + beginResult);

        response = client.sendMessage("READ 2 ;");
        int int_response = Integer.parseInt(response);
        
        boolean readResponse = int_response == 200;
        if (debug) System.out.println("read status: " + readResponse);

        client.sendMessage("ABORT ;");
        client.stop();
        return beginResult & readResponse;
    }

    public static boolean testMultipleClientTransactionRead(boolean debug) {
        long startTime = System.currentTimeMillis();
        Client client1 = new Client(port);
        Client client2 = new Client(port);
        Client client3 = new Client(port);
        boolean finalResult = true;
        System.out.println("connected new method");
        
        // Being client 1
        String response = client1.sendMessage("BEGIN 1 2 3 ;", false);
        System.out.println("sent message");
        long expiration_time = Long.parseLong(response);
        boolean beginResult = expiration_time > startTime;
        finalResult = finalResult & beginResult;
        if (debug) System.out.println("Begin client 1 status: " + beginResult);

        // Begin client 2
        response = client2.sendMessage("BEGIN 10 20 30 ;", false);
        expiration_time = Long.parseLong(response);
        beginResult = expiration_time > startTime;
        finalResult = finalResult & beginResult;
        if (debug) System.out.println("Begin client 2 status: " + beginResult);

        // Read Client 1
        response = client1.sendMessage("READ 2 ;", false);
        int intResponse = Integer.parseInt(response);
        beginResult = intResponse == 200;
        finalResult = finalResult & beginResult;
        if (debug) System.out.println("Read client 1 status: " + beginResult);

        // Begin client 3
        response = client3.sendMessage("BEGIN 100 200 300 ;", false);
        expiration_time = Long.parseLong(response);
        beginResult = expiration_time > startTime;
        finalResult = finalResult & beginResult;
        if (debug) System.out.println("Begin Client 3 Status: " + beginResult);

        // Read Client 2
        response = client2.sendMessage("READ 30 ;", false);
        intResponse = Integer.parseInt(response);
        beginResult = intResponse == 3000;
        finalResult = finalResult & beginResult;
        if (debug) System.out.println("Read client 2 status: " + beginResult);

        // Read Client 3
        response = client3.sendMessage("READ 100 ;", false);
        intResponse = Integer.parseInt(response);
        beginResult = intResponse == 10000;
        finalResult = finalResult & beginResult;
        if (debug) System.out.println("Read client 3 status: " + beginResult);

        // Abort
        client1.sendMessage("ABORT ;");
        client2.sendMessage("ABORT ;");
        client3.sendMessage("ABORT ;");
        System.out.println("Stopping clients");
        client1.stop();
        client2.stop();
        client3.stop();
        if (debug) System.out.println("Finished aborting");
        return finalResult;
    }

    public static boolean testBadRequest(boolean debug) {
        Client client1 = new Client(port);
        boolean finalResult = true;
        try { 
            client1.sendMessage("ABORT ;");
        } catch(Exception e) {
            finalResult = false;
        }
        client1.stop();

        Client client2 = new Client(port);
        try {
            client2.sendMessage("BEGIN 1 2 3 ;");
            client2.sendMessage("READ 2 ;");
            client2.sendMessage("COMMIT ;");
        } catch(Exception e) {
            finalResult = false;
        }
        client2.stop();
        return finalResult;
    }
}
