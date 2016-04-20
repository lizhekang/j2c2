package com.lk.j2c2.test;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by lizhe on 2016/4/20.
 */
public class WriteCSV {
    private String path;
    private File csv;
    private BufferedWriter bw;

    public void write(String[] data) {
        if(path == null) {
            System.out.println("没有指定输出文件名");
            return;
        }
        String fd = "";
        for(int i = 0; i < data.length; i++) {
            if(i != data.length - 1) {
                fd += data[i] + ",";
            }else {
                fd += data[i];
            }
        }

        try {
            // 新增一行数据
            bw.write(fd);
            bw.newLine();
        } catch (FileNotFoundException e) {
            // 捕获File对象生成时的异常
            e.printStackTrace();
        } catch (IOException e) {
            // 捕获BufferedWriter对象关闭时的异常
            e.printStackTrace();
        }
    }

    public void open() {
        try {
            // 打开文件
            this.csv = new File(path); // CSV文件
            this.bw = new BufferedWriter(new FileWriter(csv, true));
        } catch (IOException e) {
            // 捕获BufferedWriter对象关闭时的异常
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            // 关闭文件
            bw.close();
        } catch (IOException e) {
            // 捕获BufferedWriter对象关闭时的异常
            e.printStackTrace();
        }

    }

    public WriteCSV(String path) {
        String tp = path;
        String file = tp.substring(tp.lastIndexOf("\\")+1);
        path = tp.substring(0, tp.lastIndexOf("\\"));
        String fileName = file.substring(0, file.indexOf("."));
        String fileExpand = file.substring(file.indexOf(".")+1);

        SimpleDateFormat sdf = new SimpleDateFormat("", Locale.SIMPLIFIED_CHINESE);
        sdf.applyPattern("yyyy-MM-dd_HH-mm-ss");
        String timeStr = sdf.format(new Date());

        String np = path + "\\" + fileName + timeStr + "." + fileExpand;

        this.path = np;
    }
}
