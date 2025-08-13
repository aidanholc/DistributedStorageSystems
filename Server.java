import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeSet;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.io.IOException;
import java.net.InetSocketAddress;

public class Server {
    private ServerAdapter serverAdapter;
    private int port;
    private int nextTransactionID;
    private static final int TIMEOUT = 2_000_000; // Timeout for client requests in milliseconds
    private HashMap<Integer, List<Object>>  currentTransactions;
    private TreeSet<Integer> timedOutTransactions;

    public Server(String BTreePath, String WALPath, int port) throws IOException {
        this.serverAdapter = new ServerAdapter(BTreePath, WALPath);
        this.nextTransactionID = 0;
        this.port = port;
        this.currentTransactions = new HashMap<>();
        this.timedOutTransactions = new TreeSet<>();
    }

    public void start() throws IOException{
        Selector selector = Selector.open();
        ServerSocketChannel serverSocket = ServerSocketChannel.open();
        serverSocket.bind(new InetSocketAddress("localhost", this.port));
        serverSocket.configureBlocking(false);
        serverSocket.register(selector, SelectionKey.OP_ACCEPT);
        Long checkTimeout = System.currentTimeMillis() + TIMEOUT;

        while (true) {
            selector.select();
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectedKeys.iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                // Remove processed key
                iterator.remove();

                // Check Timeouts
                if (System.currentTimeMillis() > checkTimeout) {
                    // extend checkTimeout
                    checkTimeout = System.currentTimeMillis() + TIMEOUT;
                    Iterator<Entry<Integer, List<Object>>> timeoutIterator = this.currentTransactions.entrySet().iterator();
                    while (timeoutIterator.hasNext()) {
                        Entry<Integer, List<Object>> entry = timeoutIterator.next();
                        Integer timeoutTransactionID = entry.getKey();
                        List<Object> timeoutData = entry.getValue();
                        this.checkTimeout(timeoutTransactionID, (Long) timeoutData.get(0), (SocketChannel) timeoutData.get(1), (ByteBuffer) timeoutData.get(2), key);
                    }
                }

                if (key.isAcceptable()) {
                    // Accept new client connection
                    SocketChannel clientChannel = serverSocket.accept();
                    clientChannel.configureBlocking(false);

                    // Register for read operations and assign a transaction ID
                    int transactionID = this.nextTransactionID++;
                    clientChannel.register(selector, SelectionKey.OP_READ, Arrays.asList((long) transactionID, (long)-1));
                    // add code to initalize data for timeout checking -- also clear timeout stuff in check timeout
                    Long acceptedStartTime = System.currentTimeMillis();
                    ByteBuffer buffer = ByteBuffer.allocate(1024);
                    Object[] timeoutData = new Object[3];
                    timeoutData[0] = acceptedStartTime;
                    timeoutData[1] = clientChannel;
                    timeoutData[2] = buffer;
                    this.currentTransactions.put(transactionID, Arrays.asList(timeoutData));             


                } else if (key.isReadable()) {
                    // Handle request from client
                    long startTime = System.currentTimeMillis();
                    SocketChannel clientChannel = (SocketChannel) key.channel();
                    List<Long> attachments = (List<Long>) key.attachment();
                    int transactionID = attachments.get(0).intValue();
                    ByteBuffer buffer = ByteBuffer.allocate(1024);


                    try {
                        int bytesRead = clientChannel.read(buffer);

                        if (bytesRead == -1) {
                            System.out.println("Client disconnected");
                            this.cleanupTransaction(transactionID, key, clientChannel);
                            continue; // no need to processe this key anymore
                        } else if(bytesRead == 0) {
                            System.out.println("No data read");
                            continue;
                        } else if (!this.bufferFull(buffer)) {
                            // don't process message if it isn't fully in the buffer
                            continue;
                        }
                    } catch(java.io.IOException e) {
                        System.out.println("Connection error: " + e.getMessage());
                        e.printStackTrace();
                        this.cleanupTransaction(transactionID, key, clientChannel);
                        continue;
                    }
                    String clientRequest = new String(buffer.array(), 0, buffer.position()).trim();
                    System.out.println("Received request: " + clientRequest);
                    String operation = clientRequest.split(" ")[0];
                    int[] arguments = new int[clientRequest.split(" ").length - 2];

                    try { 
                        for (int i = 1; i < clientRequest.split(" ").length - 1; i++) { // ignore first element (operation), ignore last element (semicolon)
                            arguments[i - 1] = Integer.parseInt(clientRequest.split(" ")[i]);
                        }
                    } catch (NumberFormatException e) {
                        System.err.println("Invalid argument from transaction " + transactionID);
                        this.abortTransaction(transactionID, operation, clientChannel, buffer, key);
                    }
                    
                    // initialize data associated with transaction ID if needed
                    serverAdapter.initializeTransaction(transactionID);

                    switch (operation) {

                        case "BEGIN": {
                            // handle begin transaction
                            boolean didLock = this.serverAdapter.lockKeys(transactionID, arguments);
                            if (!didLock) {
                                System.err.println("Transaction " + transactionID + " could not acquire locks on keys: " + Arrays.toString(arguments));
                                this.abortTransaction(transactionID, operation, clientChannel, buffer, key);
                            } else { // Successfully locked keys
                                if (this.checkTimeout(transactionID, startTime, clientChannel, buffer, key)){
                                    System.out.println("timed out");
                                    continue;
                                }
                                attachments.set(1, startTime);
                                // System.out.println(Long.toString(startTime + TIMEOUT));
                                this.writeToClient(Long.toString(startTime + TIMEOUT), clientChannel, buffer, transactionID, key);
                            }
                        } break;


                        case "WRITE": {
                            // handle write operation
                            if (arguments.length != 2) {
                                System.err.println("Invalid WRITE operation format. Expected: WRITE <key> <value> ;");
                                this.abortTransaction(transactionID, operation, clientChannel, buffer, key);
                                continue;
                            } 
                            // Check if transaction has timed out
                            long transactionStartTime = attachments.get(1) == -1 ? startTime : attachments.get(1);
                            if (this.checkTimeout(transactionID, transactionStartTime, clientChannel, buffer, key)) {
                                continue;
                            }
                            System.out.println("transaction Start time: + " + transactionStartTime);
                            try {
                                Integer return_val = this.serverAdapter.put(transactionID, arguments[0], arguments[1], attachments.get(1) != -1);
                                this.writeToClient((return_val == null ? "null" : Integer.toString(return_val)), clientChannel, buffer, transactionID, key);
                            } catch (IllegalAccessError e) {
                                this.writeToClient("Key Locked", clientChannel, buffer, transactionID, key);
                            }
                        } break;


                        case "READ": {
                            // handle read operation
                            if (arguments.length != 1) {
                                System.err.println("Invalid READ operation format. Expected: READ <key> ;");
                                this.abortTransaction(transactionID, operation, clientChannel, buffer, key);
                                continue;
                            }
                            // Check if transaction has timed out
                            //System.out.println(attachments.get(1));
                            long transactionStartTime = attachments.get(1) == -1 ? startTime : attachments.get(1);
                            if (this.checkTimeout(transactionID, transactionStartTime, clientChannel, buffer, key)) {
                                System.out.println("timed out");
                                continue;
                            }
                            try {
                                Integer return_val = this.serverAdapter.get(transactionID, arguments[0], attachments.get(1) != -1);
                                // System.out.println("return val: " + return_val);
                                this.writeToClient((return_val == null ? "null" : return_val.toString()), clientChannel, buffer, transactionID, key);
                            } catch (IllegalAccessError e) {
                                this.writeToClient("Key Locked", clientChannel, buffer, transactionID, key);
                            }
                        } break;


                        case "COMMIT": {
                            // handle commit operation
                            if (this.checkTimeout(transactionID, attachments.get(1), clientChannel, buffer, key)) {
                                continue;
                            }
                            if (this.serverAdapter.hasChanges(transactionID)) {
                                boolean commitSuccess = this.serverAdapter.commit(transactionID);
                                String hasSuccess = commitSuccess ? "SUCCESS " : "FAILURE ";
                                this.writeToClient("COMMIT " + hasSuccess + transactionID, clientChannel, buffer, transactionID, key);
                            } else {
                                this.writeToClient("NO CHANGES " + transactionID, clientChannel, buffer, transactionID, key);
                            }
                        } break;


                        case "CONTAINS": {
                            // handle contains operation
                            if (arguments.length != 1) {
                                System.err.println("Invalid CONTAINS operation format. Expected: CONTAINS <key> ;");
                                this.abortTransaction(transactionID, operation, clientChannel, buffer, key);
                                continue;
                            } 
                            // Check if transaction has timed out
                            long transactionStartTime = attachments.get(1) == -1 ? startTime : attachments.get(1);
                            if (this.checkTimeout(transactionID, transactionStartTime, clientChannel, buffer, key)) {
                                continue;
                            }
                            boolean return_val = this.serverAdapter.contains(transactionID, arguments[0]);
                            this.writeToClient((return_val ? "True" : "False"), clientChannel, buffer, transactionID, key);
                        } break;


                        case "ABORT": {
                            // handle abort operation
                            this.abortTransaction(transactionID, operation, clientChannel, buffer, key);
                        } break;

                        case "SHUTDOWN": {
                            // handle shutdown operation
                            key.interestOps(SelectionKey.OP_READ);
                            this.writeToClient(Integer.toString(1), clientChannel, buffer, transactionID, key);
                            this.cleanupTransaction(transactionID, key, clientChannel);
                        }
                        
                        
                        default: {
                            // handle unknown operation
                            System.out.println("HIT DEFAULT CASE");
                            this.abortTransaction(transactionID, operation, clientChannel, buffer, key);
                        } break;
                    }
                }
            }
        }      
    }


    private void abortTransaction(int transactionID, String operation, SocketChannel clientChannel, ByteBuffer buffer, SelectionKey key) {
        try {
            this.serverAdapter.abort(transactionID);
            this.timedOutTransactions.add(transactionID);
            this.writeToClient("ABORTING " + transactionID + ", OPERATION: " + operation, clientChannel, buffer, transactionID, key);
        } catch (Exception e) {
            this.cleanupTransaction(transactionID, key, clientChannel);
            System.err.println("Error during abort for transaction: " + transactionID);
            e.printStackTrace();
        }
    }


    private boolean writeToClient(String message, SocketChannel clientChannel, ByteBuffer buffer, int transactionID, SelectionKey key) {
        try {
            if (!clientChannel.isOpen() || !clientChannel.isConnected()) {
                System.out.println("Tried to write to client but channel is closed.");
                this.cleanupTransaction(transactionID, key, clientChannel);
                return false;
            }


            int bytesWritten = clientChannel.write(ByteBuffer.wrap((message).getBytes()));
            buffer.clear();
            if (bytesWritten == -1) {
                System.out.println("Client disconnected while writing for transaction " + transactionID);
                this.cleanupTransaction(transactionID, key, clientChannel);
                return false;
            }

            return true;
        } catch (IOException e) {
            System.out.println("Failed to write to client for transaction " + transactionID + ": " + e.getMessage());
            this.cleanupTransaction(transactionID, key, clientChannel);
            return false;
        }
    }

    private void cleanupTransaction(int transactionID, SelectionKey key, SocketChannel clientChannel) { 
        try {
            this.serverAdapter.abort(transactionID);

            this.currentTransactions.remove(transactionID);
            this.timedOutTransactions.add(transactionID);

            if (key != null && key.isValid()) { 
                key.cancel();
            }

            if (clientChannel != null && clientChannel.isOpen()) {
                clientChannel.close();
            }

            System.out.println("cleaned up transaction " + transactionID);
        } catch (IOException e) {
            System.out.println("Error cleaning up transaction " + transactionID);
            e.printStackTrace();
        }
    }

    private boolean checkTimeout(int transactionID, long startTime, SocketChannel clientChannel, ByteBuffer buffer, SelectionKey key) throws IOException {
        long currentTime = System.currentTimeMillis();
        if (this.timedOutTransactions.contains(transactionID) || currentTime - startTime > TIMEOUT) {
            boolean already_included = timedOutTransactions.add(transactionID);
            if (already_included) {
                System.out.println("transaction timedout with the new functionality");
            }

            //this.serverAdapter.abort(transactionID);
            try { 
                buffer.clear();
                buffer.flip();
                clientChannel.write(ByteBuffer.wrap(("Timed Out").getBytes()));
                clientChannel.close();
                buffer.clear();
            } catch(IOException e) {
                System.out.println("Could not send timeout message to client: " + e.getMessage());
            }
            this.cleanupTransaction(transactionID, key, clientChannel);
            return true;
        }
        return false;
    }

    private boolean bufferFull(ByteBuffer buffer) {
        String contents = new String(buffer.array(), 0, buffer.position()).trim();
        return (!contents.isEmpty() && contents.endsWith(";"));
    }
}
