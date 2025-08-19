package src;

import java.io.IOException;

public class MultiClientRunner {
    private static int serverCount = 7;


    public static void main(String[] args) throws IOException {
        System.out.println("in multi client runner");
        testDistribution(5660);
        System.out.println("Ran test");
    }


    private static boolean testDistribution(int basePort) throws IOException {
        Server[] servers = new Server[serverCount];
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
                client.getResponses(5000, 2);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return false;

    }

}

