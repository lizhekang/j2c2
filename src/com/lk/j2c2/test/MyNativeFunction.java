package com.lk.j2c2.test;

import java.util.function.Function;

/**
 * Created by lizhe on 2016/4/20.
 */
public class MyNativeFunction implements Function<int[], int[]> {
    public native int[] apply(int[] a) ;
}
