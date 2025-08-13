import java.io.IOException;

public class ServerRunner {
    public static void main(String[] args) {
        int port = 5460;
        try {
            System.out.println("in try");
            Server server = new Server("1mil.btree", "benchmark.wal", port);
            System.out.println("Initialized server");
            server.start();
        } catch (IOException e) {
            System.err.println("Server Failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}