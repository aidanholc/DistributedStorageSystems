package src;
import java.io.IOError;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class Client {
    private SocketChannel clientChannel;
    private ByteBuffer buffer;
    private int port;

    public Client(int port) {
        this.port = port;
        try {
            this.clientChannel = SocketChannel.open(new InetSocketAddress("localhost", this.port));
            this.buffer = ByteBuffer.allocate(1024);
        } catch (IOException e) {
            System.err.println("Failed to connect to server: " + e.getMessage());

            e.printStackTrace();
        }
      
    }

    public void stop() {
       // System.out.println("closing client");
        try {
            this.clientChannel.close();
            this.buffer = null;
        } catch (IOException e) {
            System.err.println("Failed to close client: " + e.getMessage());
        }
      //  System.out.println("finished closing client");
    }

    public String sendMessage(String message) {
        return this.sendMessage(message, true);
    }

    public String sendMessage(String message, boolean needFlip) {
        String response = null;
        try {
            if (needFlip) {
                this.buffer.flip();
            }
            this.clientChannel.write(ByteBuffer.wrap(message.getBytes()));
            this.buffer.clear();
            int size = this.clientChannel.read(this.buffer);
            
            if (size > 0) {
                response = new String(this.buffer.array(), 0, size).trim();
            } else if (size == -1) {
                response = "CONNECTION CLOSED";
            } else {
                response = "";
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }
}