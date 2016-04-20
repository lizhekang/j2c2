package com.lk.j2c2.test;

import java.util.Arrays;
import java.util.Random;

/**
 * Created by lizhe on 2016/4/19.
 */
public class Main {

    private static WriteCSV wc;

    private static int[][] init(int size) {
        int[][] a = new int[size][size];
        Random random = new Random();//默认构造方法

        for(int i=0; i<size; i++)
            for(int j=0; j<size; j++)
                a[i][j] = random.nextInt(size);

        return a;
    }

    private static MyFunction action_1 = new MyFunction();

    private static MyNativeFunction action_2 = new MyNativeFunction();

    private static void test(int begin, int end, int step) {
        String[] title = {"Size", "Java", "JIN"};
        wc.write(title);

        for(int i = begin; i <= end; i = i + step) {
            int[][] a = init(i);

            long startTime=System.nanoTime();
            Arrays.stream(a)
                    .map(action_1)
                    .findFirst().get();
            long endTime=System.nanoTime();
            System.out.println("Run time: "+ ( endTime - startTime ) + "ns.");
            long r1 = endTime - startTime;

            startTime=System.nanoTime();
            Arrays.stream(a)
                    .map(action_2)
                    .findFirst().get();
            endTime=System.nanoTime();
            System.out.println("Run time: "+ ( endTime - startTime ) + "ns.");
            long r2 = endTime - startTime;

            String[] res = {Integer.toString(i), Long.toString(r1 / 1000), Long.toString(r2 / 1000)};
            wc.write(res);
        }

        wc.close();
    }

    public static void main(String[] args) {
        System.loadLibrary("j2c2");

        wc = new WriteCSV("data\\result.csv");
        wc.open();
        test(1000, 20000, 1000);
    }
}
