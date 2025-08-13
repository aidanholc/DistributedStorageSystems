import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class ProtocolTest {
    private static final String HOST = "localhost";
    private static final int PORT = 8080;
    
    public static void main(String[] args) {
        System.out.println("=== Key-Value Store Protocol Test ===");
        
        // Test 1: Basic operations
        testBasicOperations();
        
        // Test 2: Transaction with writes
        testTransactionWithWrites();
        
        // Test 3: Read-only transaction
        testReadOnlyTransaction();
        
        // Test 4: Transaction locking
        testTransactionLocking();
        
        // Test 5: Transaction timeout
        testTransactionTimeout();
        
        // Test 6: Error handling
        testErrorHandling();
        
        System.out.println("\n=== All tests completed ===");
    }
    
    private static void testBasicOperations() {
        System.out.println("\n--- Test 1: Basic Operations ---");
        
        try (Client client = new Client(HOST, PORT)) {
            client.connect();
            
            // Test PUT
            String response = client.put("test1", "value1");
            System.out.println("PUT test1 value1: " + response);
            
            // Test GET
            response = client.get("test1");
            System.out.println("GET test1: " + response);
            
            // Test CONTAINS
            response = client.contains("test1");
            System.out.println("CONTAINS test1: " + response);
            
            response = client.contains("nonexistent");
            System.out.println("CONTAINS nonexistent: " + response);
            
        } catch (Exception e) {
            System.err.println("Basic operations test failed: " + e.getMessage());
        }
    }
    
    private static void testTransactionWithWrites() {
        System.out.println("\n--- Test 2: Transaction with Writes ---");
        
        try (Client client = new Client(HOST, PORT)) {
            client.connect();
            
            // Start transaction
            String response = client.startTransaction("key1", "key2", "key3");
            System.out.println("START TRANSACTION key1 key2 key3: " + response);
            
            // Perform writes
            response = client.put("key1", "value1");
            System.out.println("PUT key1 value1: " + response);
            
            response = client.put("key2", "value2");
            System.out.println("PUT key2 value2: " + response);
            
            response = client.put("key1", "updated_value1");
            System.out.println("PUT key1 updated_value1: " + response);
            
            // Commit transaction
            response = client.commit();
            System.out.println("COMMIT: " + response);
            
            // Verify results
            response = client.get("key1");
            System.out.println("GET key1: " + response);
            
            response = client.get("key2");
            System.out.println("GET key2: " + response);
            
        } catch (Exception e) {
            System.err.println("Transaction with writes test failed: " + e.getMessage());
        }
    }
    
    private static void testReadOnlyTransaction() {
        System.out.println("\n--- Test 3: Read-Only Transaction ---");
        
        try (Client client = new Client(HOST, PORT)) {
            client.connect();
            
            // First, put some values
            client.put("read_key1", "read_value1");
            client.put("read_key2", "read_value2");
            
            // Start transaction
            String response = client.startTransaction("read_key1", "read_key2");
            System.out.println("START TRANSACTION read_key1 read_key2: " + response);
            
            // Perform reads
            response = client.get("read_key1");
            System.out.println("GET read_key1: " + response);
            
            response = client.get("read_key2");
            System.out.println("GET read_key2: " + response);
            
            // Abort transaction (no writes to commit)
            response = client.abort();
            System.out.println("ABORT: " + response);
            
        } catch (Exception e) {
            System.err.println("Read-only transaction test failed: " + e.getMessage());
        }
    }
    
    private static void testTransactionLocking() {
        System.out.println("\n--- Test 4: Transaction Locking ---");
        
        try (Client client1 = new Client(HOST, PORT);
             Client client2 = new Client(HOST, PORT)) {
            
            client1.connect();
            client2.connect();
            
            // Client 1 starts transaction and locks keys
            String response = client1.startTransaction("lock_key1", "lock_key2");
            System.out.println("Client1 START TRANSACTION lock_key1 lock_key2: " + response);
            
            // Client 2 tries to start transaction with same keys (should fail)
            response = client2.startTransaction("lock_key1", "lock_key2");
            System.out.println("Client2 START TRANSACTION lock_key1 lock_key2: " + response);
            
            // Client 2 tries to access locked keys directly (should fail)
            response = client2.put("lock_key1", "blocked_value");
            System.out.println("Client2 PUT lock_key1 blocked_value: " + response);
            
            // Client 1 commits, releasing locks
            response = client1.commit();
            System.out.println("Client1 COMMIT: " + response);
            
            // Now Client 2 should be able to access the keys
            response = client2.startTransaction("lock_key1", "lock_key2");
            System.out.println("Client2 START TRANSACTION lock_key1 lock_key2: " + response);
            
        } catch (Exception e) {
            System.err.println("Transaction locking test failed: " + e.getMessage());
        }
    }
    
    private static void testTransactionTimeout() {
        System.out.println("\n--- Test 5: Transaction Timeout ---");
        
        try (Client client = new Client(HOST, PORT)) {
            client.connect();
            
            // Start transaction
            String response = client.startTransaction("timeout_key");
            System.out.println("START TRANSACTION timeout_key: " + response);
            
            // Put a value
            response = client.put("timeout_key", "timeout_value");
            System.out.println("PUT timeout_key timeout_value: " + response);
            
            // Wait for transaction to expire (35 seconds)
            System.out.println("Waiting 35 seconds for transaction timeout...");
            Thread.sleep(35000);
            
            // Try to access the key (should fail due to expired transaction)
            response = client.put("timeout_key", "new_value");
            System.out.println("PUT timeout_key new_value: " + response);
            
        } catch (Exception e) {
            System.err.println("Transaction timeout test failed: " + e.getMessage());
        }
    }
    
    private static void testErrorHandling() {
        System.out.println("\n--- Test 6: Error Handling ---");
        
        try (Client client = new Client(HOST, PORT)) {
            client.connect();
            
            // Test invalid commands
            String response = client.sendCommand("INVALID_COMMAND");
            System.out.println("INVALID_COMMAND: " + response);
            
            response = client.sendCommand("PUT");
            System.out.println("PUT (no args): " + response);
            
            response = client.sendCommand("GET");
            System.out.println("GET (no args): " + response);
            
            // Test commit without transaction
            response = client.commit();
            System.out.println("COMMIT (no transaction): " + response);
            
            // Test abort without transaction
            response = client.abort();
            System.out.println("ABORT (no transaction): " + response);
            
            // Test commit with empty transaction
            response = client.startTransaction("empty_key");
            System.out.println("START TRANSACTION empty_key: " + response);
            
            response = client.commit();
            System.out.println("COMMIT (no writes): " + response);
            
        } catch (Exception e) {
            System.err.println("Error handling test failed: " + e.getMessage());
        }
    }
    
    // Simple client wrapper for testing
    private static class Client implements AutoCloseable {
        private final String host;
        private final int port;
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        
        public Client(String host, int port) {
            this.host = host;
            this.port = port;
        }
        
        public void connect() throws IOException {
            socket = new Socket(host, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
        }
        
        public String sendCommand(String command) throws IOException {
            out.println(command);
            return in.readLine();
        }
        
        public String put(String key, String value) throws IOException {
            return sendCommand("PUT " + key + " " + value);
        }
        
        public String get(String key) throws IOException {
            return sendCommand("GET " + key);
        }
        
        public String contains(String key) throws IOException {
            return sendCommand("CONTAINS " + key);
        }
        
        public String startTransaction(String... keys) throws IOException {
            StringBuilder command = new StringBuilder("START TRANSACTION");
            for (String key : keys) {
                command.append(" ").append(key);
            }
            return sendCommand(command.toString());
        }
        
        public String commit() throws IOException {
            return sendCommand("COMMIT");
        }
        
        public String abort() throws IOException {
            return sendCommand("ABORT");
        }
        
        @Override
        public void close() {
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                System.err.println("Error closing client: " + e.getMessage());
            }
        }
    }
} 