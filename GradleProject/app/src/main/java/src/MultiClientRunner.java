package src;

import java.io.IOException;

public class MultiClientRunner {
    private static int serverCount = 7;


    public static void main(String[] args) throws IOException {
        System.out.println("in multi client runner");
        //testDistribution(5660);
        testConsistentReads(5660);
        System.out.println("Ran test");
    }


    private static boolean testDistribution(int basePort) throws IOException {
        String[] serverNames = new String[serverCount];
        MultiClient client;
        // initialize servers
        for (int i = 0; i < serverCount; i++) {
            int curPort = basePort + i;
            serverNames[i] = "localhost:" + curPort;
           // servers[i] = new Server(curPort + ".btree", curPort + ".wal", curPort);

        }
        client = new MultiClient(3, serverNames);
        System.out.println("middle of test dist");
        // send data to servers
        for(int i = 100_000; i < 110_001; i++) {
            System.out.println("Outside of try catch");
            try {
                System.out.println("in for loop");
                client.sendWrite(i, i*100, 2);
                int response = client.getResponses(5000, 2);
                System.out.println(response);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return false;

    }
    private static boolean testConsistentReads(int basePort) throws IOException { 
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

