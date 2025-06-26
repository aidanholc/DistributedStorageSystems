import java.util.ArrayList;
import java.util.Random;
import java.util.List;
import java.util.Collections;
import java.lang.Math;


class Main {
    public static void main(String[] args) {
        System.out.println("Starting main method...");
        // This is a simple Java program that prints "Hello, World!" to the console.
        int[] tests = {1000, 5000, 10000, 50000, 100000, 500000};
        for (int test : tests) {
            long duration = test_write(test);
            System.out.println("Time taken to write " + test + " keys: " + duration + " milliseconds");
        } 

        // for (int test : tests) {
        //     long duration = test_read(test);
        //     System.out.println("Time taken to read " + test + " keys: " + duration + " milliseconds");
        // }
    }


    public static long test_write(int num_keys) {
        BTree b = new BTree("write_test.btree");
        long[] durations = new long[3];
        for (int j = 0; j < 3; j++){
            long start = System.nanoTime();
            for (int i = 0; i < num_keys; i++) {
                b.insert((int) (Math.random() * 1_000_000), (int) (Math.random() * 1_000_000));
            }
            long end = System.nanoTime();
            long duration = (end - start);
            durations[j] = duration;
        }
        return ((durations[0] / 3) + (durations[1] / 3) + (durations[2] / 3)) / 1_000_000; // average the durations and convert to milliseconds
    }

    public static long test_read(int num_keys) {
        BTree b = new BTree("write_test.btree");
        long[] durations = new long[3];
        for (int j = 0; j < 3; j++){
            long start = System.nanoTime();
            for (int i = 0; i < num_keys; i++) {
                b.get((int) (Math.random() * 1_000_000));
            }
            long end = System.nanoTime();
            long duration = (end - start);
            durations[j] = duration;
        }
        return ((durations[0] / 3) + (durations[1] / 3) + (durations[2] / 3)) / 1_000_000; // average the durations and convert to milliseconds
    }
}