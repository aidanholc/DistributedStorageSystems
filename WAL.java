import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class WAL {
    private PrintWriter writer;
    public WAL(String filePath) {
        try {
            this.writer = new PrintWriter(new FileWriter(filePath, true));
        } catch (IOException e) {
            System.err.println("Error initializing WAL: " + e.getMessage());
        }
    }
    
    public void write(int transactionID, int key, int newValue, Integer oldValue){
        if (oldValue == null) {
            this.writer.print("W" + transactionID + ":" +  + key + "," + newValue + ",null;");
        }
            this.writer.print("W" + transactionID + ":" +  + key + "," + newValue + "," + oldValue + ";");
        this.writer.flush();
    }

    public void commit(int transactionID) {
        this.writer.print("C" + transactionID + ":;");
        this.writer.flush();
    }

    public void abort(int transactionID) {
        this.writer.print("A" + transactionID + ":;");
        this.writer.flush();
    }

    public void transaction(int transactionID) {
        this.writer.print("T" + transactionID + ":;");
        this.writer.flush();
    }

    public void checkpoint() {
        this.writer.print("P;");
        this.writer.flush();
    }

    public void close() {
        this.writer.close();
    }
}
