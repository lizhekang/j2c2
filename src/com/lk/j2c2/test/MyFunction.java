package com.lk.j2c2.test;

import java.util.function.Function;

/**
 * Created by lizhe on 2016/4/20.
 */
public class MyFunction implements Function<int[], int[]> {
    public int[] apply(int[] a) {
        int temp = 0;
        for(int i = 0; i < a.length; i++){
            for(int j = i+1; j < a.length; j++){
                if(a[i] > a[j]){
                    temp = a[i];
                    a[i] = a[j];
                    a[j] = temp;
                }
            }
        }
        return a;
    }

    public double test(double[] b, int a) {
        return 0.1;
    }
}
