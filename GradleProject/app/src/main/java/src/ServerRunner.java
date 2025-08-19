package src;
import java.io.IOException;
import java.util.Arrays;

public class ServerRunner {
    public static void main(String[] args) {
        int port = Integer.parseInt(args[0]);
        //int id = Integer.parseInt(args[1]);
        try {
            Server server = new Server(port + ".btree", port + ".wal", port);
            System.out.println("Initialized server");
            server.start();
        } catch (IOException e) {
            System.err.println("Server Failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}