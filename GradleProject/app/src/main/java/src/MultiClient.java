package src;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.codec.digest.MurmurHash3;

public class MultiClient {
    private int quorumSize;
    private TreeMap<String, ServerInfo> connectionData;
    private Selector selector;
    private String currentMessage;

    private static class ServerInfo { 
        private final SocketChannel channel;
        private ByteBuffer buffer;
        private StringBuilder response;
        private boolean waiting;
        private final String server;

        ServerInfo(String server, SocketChannel channel) {
            this.server = server;
            this.channel = channel;
            this.response = new StringBuilder();
            this.waiting = false;
        }
    }

    public MultiClient(int quorumSize, String[] servers) {
        if (quorumSize <= 0 || servers.length < quorumSize) {
            throw new IllegalArgumentException("Number of servers must be greater than or equal to quorum size");
        }
        this.currentMessage = "";
        this.connectionData = new TreeMap<>();
        this.quorumSize = quorumSize;

        try {
            this.selector = Selector.open();
            for(int i = 0; i < servers.length; i++) {
                int colonIndex = servers[i].lastIndexOf(':');
                if (colonIndex == -1) {
                    throw new IllegalArgumentException("Cannot connect to server: " + servers[i] + ", format must be address:port");
                }
                String host = servers[i].substring(0, colonIndex);
                int port = Integer.parseInt(servers[i].substring(colonIndex + 1));
                SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress(host, port));
                socketChannel.configureBlocking(false);
                ServerInfo info = new ServerInfo(servers[i], socketChannel);
                this.connectionData.put(servers[i], info);
                socketChannel.register(this.selector, SelectionKey.OP_READ, info);
            }
        } catch(IOException e) {
            System.err.println("Unable to start client");
            e.printStackTrace();
        }
    }

    public boolean sendRead(int key, int readQuorum) throws IOException{
        String message = "READ " + key + " ;";
        return this.sendMessage(message, readQuorum, -1, true);
    }

    public boolean sendWrite(int key, int value, int writeQuorum) throws IOException {
        String message = "WRITE " + key + " " + value + " ;";
        return this.sendMessage(message, -1, writeQuorum, false);
    }

    private boolean sendMessage(String message, int readQuorum, int writeQuorum, boolean isRead) throws IOException {
        this.currentMessage = message;
        int dataKey = Integer.parseInt(message.split(" ")[1]); // can only get here if message is valid format
        String[] servers = this.rendezvousHash(dataKey);
        String[] topServers = Arrays.copyOfRange(servers, 0, this.quorumSize);
        int failedServers = 0;
        for (String server: topServers) {
            ServerInfo info = connectionData.get(server);

            if(info.waiting) {
                failedServers++;
                continue ; // If this server is still handling a request from this client just continue
            }

            ByteBuffer buffer = ByteBuffer.wrap(message.getBytes());
            info.buffer = buffer;

            SelectionKey key = info.channel.keyFor(this.selector);
            if (key != null && key.isValid()) {
                key.interestOps(key.interestOps() | SelectionKey.OP_WRITE); // Make sure the channel is actually writable
                int bytesWritten = info.channel.write(info.buffer);
                info.waiting = true;
                key.interestOps(SelectionKey.OP_READ); // Turn back to reading now that the data is written
                if (bytesWritten != message.getBytes().length){
                    System.out.println("didn't write enough data ;(");
                    failedServers++;
                }
            }
        }

        // Make sure enough data was written where its possible to have a quorum consensus
        if (isRead) {
            return (quorumSize - failedServers) >= readQuorum;
        } else {
            return (quorumSize - failedServers) >= writeQuorum;
        }
    }

    public int getResponses(long timeout, int quorum) throws IOException {
        boolean timedOut = false;
        TreeMap<Integer, Integer> readCounts = new TreeMap<Integer, Integer>();
        long startTime = System.currentTimeMillis();
        int curQuorum = 0;
        int acceptedValue = -1;

        while (!timedOut) {
            int readyChannels = this.selector.select(1000);
            if (readyChannels == 0) {
                long currentTime = System.currentTimeMillis();
                timedOut = (currentTime - startTime) > timeout;
                continue;
            }

            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectedKeys.iterator();

            while(!timedOut && iterator.hasNext()) {
               /// System.out.println("inner while loop");
                long currentTime = System.currentTimeMillis();
                timedOut = (currentTime - startTime) > timeout;

                SelectionKey key = iterator.next();
                iterator.remove();

                if (!key.isValid()) {
                    continue;
                }

                SocketChannel channel = (SocketChannel) key.channel();
                ServerInfo info = (ServerInfo) key.attachment();

                try {
                    if (key.isReadable()) {
                        ByteBuffer buffer = ByteBuffer.allocate(1024);
                        int bytesRead = channel.read(buffer);

                        if (bytesRead > 0) {
                            buffer.flip();
                            String response = new String(buffer.array(), 0, buffer.remaining());
                            info.response.append(response);

                            if (info.response.toString().endsWith(";")) { // full message sent
                                response  = info.response.toString();
                                info.response = new StringBuilder();
                                info.waiting = false;

                                // Processe  Response
                                String returnValue = response.split(" ")[0];
                                if (returnValue.equals("null")) {
                                    Integer curCount = readCounts.get(Integer.MIN_VALUE);
                                    if(curCount == null) {
                                        curCount = 0;
                                    }
                                    curCount++;
                                    readCounts.put(Integer.MIN_VALUE, curCount);
                                } else if (returnValue.equals("Key") || returnValue.equals("ABORTING")) { // hacky, but the only other options other than an integer
                                    // handle failed operation
                                    System.out.println("Server " + info.server + " returned an error: " + response);
                                    info.waiting = false;
                                } else {
                                    int value = Integer.parseInt(returnValue);
                                    Integer curCount = readCounts.get(value);
                                    if (curCount == null) {
                                        curCount = 0;
                                    }
                                    curCount++;
                                    readCounts.put(value, curCount);
                                }
                            }
                        } else if (bytesRead == -1){
                            info.waiting = false;
                            System.out.println("Server Closed");
                        }
                    }
                } catch (IOException e) {
                    key.cancel();
                    channel.close();
                    info.waiting = false;
                    System.out.println("Caught Exception");
                    e.printStackTrace();
                }
                for(Map.Entry<Integer, Integer> entry: readCounts.entrySet()) {
                    int value = entry.getKey();
                    int count = entry.getValue();
                    if (count > curQuorum) {
                        curQuorum = count;
                        acceptedValue = value;
                    }
                    if (curQuorum >= quorum) {
                        return acceptedValue; // return immediately if a quorum was reached.
                    }
                }
                System.out.println("end of while loop");

            }
        }
        // If no quorum was reached, return a signal to show that.
        return Integer.MAX_VALUE;
    }
    private String[] rendezvousHash(int key) {
        TreeMap<Integer, String> hashes = new TreeMap<Integer, String>();
        String[] servers = this.connectionData.keySet().toArray(new String[0]);
        for (int i = 0; i < servers.length; i++) {
            String input = key + servers[i];
            byte[] data = input.getBytes(StandardCharsets.UTF_8);
            int hash = MurmurHash3.hash32x86(data, 0, data.length, 42);
            hashes.put(hash, servers[i]);
        }
        return hashes.values().toArray(new String[0]);
    }
}
 