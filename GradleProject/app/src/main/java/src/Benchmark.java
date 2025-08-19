package src;
public class Benchmark {
    public static void main(String[] args) {
        int port = 5460;
        // reserve 100 keys at a time to reduce the chances of having conflicting keys
        Client client = new Client(port);
        long startTime = System.currentTimeMillis();
        for(int i = 0; i < 10_000; i++) { 
            if (i % 1_000 == 0) {
                System.out.println((double)i / 10_000);
            }
            int randNum = (int)(Math.random() * 1_000_000);
            String response = client.sendMessage("READ " + randNum + " ;");
            if (response.equals("null" )){ 
                System.out.println("Returned null");
            }
            try {
                Thread.sleep(20);
            } catch(InterruptedException e) {
                e.printStackTrace();
            }
        }
        long endTime = System.currentTimeMillis();

        double duration = (double)(endTime - startTime) / 1_000;
        System.out.println("Requests Per Second: "  + 10_000 / duration);
        client.stop();
    }
}
