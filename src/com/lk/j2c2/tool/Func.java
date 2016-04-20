package com.lk.j2c2.tool;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by lizhe on 2016/4/20.
 */
class Func {
    private String classFileOutputPath;
    private String functionPrototype;

    private final Map<String, String> KEYWORDS = new HashMap<>();
    private final Map<String, String> VALUES = new HashMap<>();

    private FunctionInfo functionInfo;

    private class FunctionInfo {
        String functionName;
        int stack;
        int args_size;
        String returnKind;
        String[] argKinds;
        String[] codes;
        Map<Integer, FuncValue> values;
    }

    private class FuncValue {
        String name;
        String kind;
        boolean define;

        FuncValue(String name, String kind) {
            this.name = name;
            this.kind = kind;
            this.define = false;
        }
    }

    private class FuncException extends Exception {
        FuncException(String msg) {
            super(msg);
        }
    }


    Func(String classFileOutputPath, String functionPrototype) {
        this.classFileOutputPath = classFileOutputPath;
        this.functionPrototype = functionPrototype;

        this.functionInfo = new FunctionInfo();

        // 初始化关键字
        KEYWORDS.put("descriptor", "descriptor");
        KEYWORDS.put("flags", "flags");
        KEYWORDS.put("Code", "Code");
        KEYWORDS.put("LineNumberTable", "LineNumberTable");
        KEYWORDS.put("LocalVariableTable", "LocalVariableTable");
        KEYWORDS.put("StackMapTable", "StackMapTable");

        // 初始化变量对应表
        VALUES.put("B", "byte");
        VALUES.put("C", "char");
        VALUES.put("D", "double");
        VALUES.put("F", "float");
        VALUES.put("I", "int");
        VALUES.put("J", "long");
        VALUES.put("S", "short");
        VALUES.put("Z", "boolean");
        VALUES.put("V", "void");
        //TODO: 处理对象
        //VALUES.put("L", "");

        functionInfo.values = new HashMap<>();
    }

    private String[] read(String fileName) {
        File file = new File(fileName);
        BufferedReader reader = null;

        ArrayList<String> sList = new ArrayList<>();

        try {
            reader = new BufferedReader(new FileReader(file));
            String tempString = null;

            while ((tempString = reader.readLine()) != null) {
                sList.add(tempString);
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }

        return sList.toArray(new String[sList.size()]);
    }

    private String[] getTargetFunction(String[] origin, String target) {
        ArrayList<String> sList = new ArrayList<>(Arrays.asList(origin));
        ArrayList<String> rList = new ArrayList<>();

        boolean beginFlag = false;
        for (String s: sList) {
            if(s.contains(target)) {
                beginFlag = true;
            }
            if(s.equals("") && beginFlag) {
                break;
            }
            if(beginFlag) {
                rList.add(s.trim());
            }
        }

        return rList.toArray(new String[rList.size()]);
    }

    private void getTargetFunctionDes(String[] function) throws FuncException {
        String des = "";

        for(String s:function) {
            if(s.contains(KEYWORDS.get("descriptor") + ":")) {
                des = s;
                break;
            }
        }

        if(des.equals("")) {
            throw new FuncException("不能获取方法体描述");
        }else {
            // 获取方法体信息
            des = des.split(":")[1].trim();
            String argsStr = des.substring(des.indexOf("(") + 1, des.indexOf(")"));
            String returnStr = (des.indexOf("(") == 0) ? des.substring(des.indexOf(")") + 1) : des.substring(0, des.indexOf("("));

            functionInfo.returnKind = kindIdentifier(returnStr);

            ArrayList<String> tas = new ArrayList<>();
            char[] tc = argsStr.toCharArray();
            String ts = "";
            for(char c:tc) {
                if(c == '[') {
                    ts += c;
                }else if(VALUES.get(String.valueOf(c)) != null) {
                    ts += c;
                    tas.add(kindIdentifier(ts));
                    ts = "";
                }else {
                    throw new FuncException("无法识别类型");
                }
            }

            functionInfo.argKinds = tas.toArray(new String[tas.size()]);
        }
    }

    private String kindIdentifier(String k) throws FuncException{
        int counter = 0;
        char[] ar = k.toCharArray();
        String res = "";

        for(char c:ar) {
            if(c == '[') {
                counter++;
            }else {
                if(VALUES.get(String.valueOf(c)) != null) {
                    res += VALUES.get(String.valueOf(c));
                    for(int i=0; i<counter; i++) {
                        res += "[]";
                    }
                }else {
                    throw new FuncException("无法识别类型");
                }
            }
        }

        return res;
    }

    private void getTargetFunctionCode(String[] function) throws FuncException {
        boolean beginFlag = false;
        ArrayList<String> aCodes = new ArrayList<>();
        String infoPatStr = "((\\w|_)+=(\\d)+(,\\s)*)+";
        Pattern infoPat = Pattern.compile(infoPatStr);
        String codePatStr = "^(\\d)+:(\\s)+(\\w|_)+";
        Pattern codePat = Pattern.compile(codePatStr);

        for(String s:function) {
            if(s.contains(KEYWORDS.get("Code") + ":")) {
                beginFlag = true;
                continue;
            }
            if(beginFlag) {
                if(codePat.matcher(s).find()) {
                    aCodes.add(s);
                }else if(infoPat.matcher(s).find()) {
                    getTargetFunctionCodeInfo(s);
                }
            }
        }

        if(!beginFlag)
            throw new FuncException("不能获取代码信息");

        functionInfo.codes = aCodes.toArray(new String[aCodes.size()]);
    }

    private void getTargetFunctionName(String[] function) throws FuncException {
        String t = function[0];
        String pat = "(\\w)*(\\()";
        Pattern r = Pattern.compile(pat);
        Matcher m = r.matcher(t);

        if(m.find()) {
            String res = m.group(0);
            functionInfo.functionName = res.substring(0, res.length() - 1);
        }else {
            throw new FuncException("不能获取文件名");
        }
    }

    private void getTargetFunctionCodeInfo(String infos) throws FuncException {
        String[] aInfos = infos.split(",");

        for(String s:aInfos) {
            s = s.trim();
            String[] vk = s.split("=");

            if(vk.length != 2) {
                throw new FuncException("不能获取函数信息");
            }else {
                switch (vk[0]) {
                    case "stack":
                        functionInfo.stack = Integer.parseInt(vk[1]);
                        break;
                    case "args_size":
                        functionInfo.args_size = Integer.parseInt(vk[1]);
                        break;
                    default:
                        break;
                }
            }
        }
    }

    private void getTargetFunctionValueTable(String[] function) throws FuncException {
        boolean beginFlag = false;
        boolean beginTableFlag = false;
        String tablePatStr = "(\\d+(\\s)*){3}([\\w/;\\[]+(\\s)*){2}";
        Pattern tablePat = Pattern.compile(tablePatStr);

        for(String s:function) {
            if(s.contains(KEYWORDS.get("LocalVariableTable") + ":")) {
                beginFlag = true;
                continue;
            }
            if(beginFlag && tablePat.matcher(s).find()) {
                beginTableFlag = true;

                String[] tsa = s.split("\\s+");
                functionInfo.values.put(new Integer(tsa[2]), new FuncValue(tsa[3], tsa[4]));
            }else if(beginFlag && beginTableFlag) {
                break;
            }
        }
    }

    private String getCFunctionCode() {
        String functionHead = "#include <stdio.h>\n";
        String functionBody = "";

        //组装函数头
        functionHead += functionInfo.returnKind + " " + functionInfo.functionName + "(";
        for(int i=0; i<functionInfo.argKinds.length; i++) {
            functionHead += functionInfo.argKinds[i] + " " + functionInfo.values.get(i+1).name;
            functionInfo.values.get(i+1).define = true;
            if(i != functionInfo.argKinds.length - 1) {
                functionHead += ", ";
            }
        }
        functionHead += ")\n";
        functionBody += "{\n";
        functionBody += "}\n";

        System.out.println(functionHead + functionBody);
        return functionHead + functionBody;
    }

    //TODO: clean
    public void test() {
        try {
            String[] oList = read(classFileOutputPath);
            String[] targetFunction = getTargetFunction(oList, functionPrototype);
            // 获取方法名
            getTargetFunctionName(targetFunction);
            // 获取方法描述
            getTargetFunctionDes(targetFunction);
            // 获取代码信息
            getTargetFunctionCode(targetFunction);
            // 获取变量信息
            getTargetFunctionValueTable(targetFunction);

            getCFunctionCode();

        }catch (FuncException fe) {
            fe.printStackTrace();
        }
    }
}
