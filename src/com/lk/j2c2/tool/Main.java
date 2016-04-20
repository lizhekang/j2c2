package com.lk.j2c2.tool;

import java.io.IOException;

/**
 * Created by lizhe on 2016/4/20.
 */
public class Main {
    private static Runtime runtime;

    private static void init() {
        runtime = Runtime.getRuntime();
    }

    private static void createClassFile(String inputFile, String outputFile) {
        try {
            System.out.println("Begin to create a class file.");
            runtime.exec("createClassFile.bat " + inputFile + " " + outputFile);
            System.out.println("Successfully created.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    public static void main(String[] argv) {
        String classPath = argv[0];
        String classFileOutputPath = argv[1];
        String functionPrototype = argv[2];
        String cppFileOutputPath = argv[3];

        init();
        createClassFile(classPath, classFileOutputPath);

        Func applyFunc = new Func(classFileOutputPath, functionPrototype);
        applyFunc.test();
    }
}
