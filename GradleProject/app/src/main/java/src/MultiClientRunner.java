package src;

import java.io.IOException;

public class MultiClientRunner {
    /**
     * 
     * Write Times
     *   Quorum: 3
     *          59688 ms
     *   Quorum: 5
     *      271472 ms
     * 
     *  Read Times
     *   11 7 5 - 41226
     *   
     * 
     */



    public static void main(String[] args) throws IOException {
        int serverCount = Integer.parseInt(args[0]);
        int r = Integer.parseInt(args[1]);
        int readWriteQuorum = Integer.parseInt(args[2]);
        System.out.println("in multi client runner");
        testDistribution(5660, serverCount, r, readWriteQuorum);
        // testConsistentReads(5660);
        System.out.println("Ran test");
    }

    private static boolean testDistribution(int basePort, int serverCount, int r, int readWriteQuorum) throws IOException {
        String[] serverNames = new String[serverCount];
        MultiClient client;
        // initialize servers
        for (int i = 0; i < serverCount; i++) {
            int curPort = basePort + i;
            serverNames[i] = "localhost:" + curPort;
           // servers[i] = new Server(curPort + ".btree", curPort + ".wal", curPort);

        }
        client = new MultiClient(r, serverNames);
        System.out.println("middle of test dist");
        // send data to servers 
        long startTime = System.currentTimeMillis();
        for(int i = 0; i < 10_000; i++) {
            try {
                client.sendRead(i, readWriteQuorum);
                int response = client.getResponses(2000, readWriteQuorum);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        long endTime = System.currentTimeMillis();
        System.out.println("RUNTIME: " + (endTime - startTime));

        return false;

    }
    private static boolean testConsistentReads(int basePort, int serverCount) throws IOException { 
        String[] serverNames = new String[serverCount];
        for(int i = 0; i < serverCount; i++) {
            int curPort = basePort + i;
            Client client = new Client(curPort);
            client.sendMessage("WRITE 1 " + curPort + " ;");
            client.stop();
            
            serverNames[i] = "localhost:" + curPort;
        }

        MultiClient client = new MultiClient(3, serverNames);
        boolean response = client.sendRead(1, 3);
        System.out.println("result of sendRead: " + response);
        int nextResponse = client.getResponses(5000, 3);
        System.out.println("result of getResponses: " + nextResponse);

        return response;
    }
}

