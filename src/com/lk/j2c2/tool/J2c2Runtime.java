package com.lk.j2c2.tool;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by lizhe on 2016/4/23.
 */
class J2c2Runtime {
    private Stack<String> stack;
    private String[] codes;
    private List<String> result;
    private Map<Integer, Func.FuncValue> vm;
    private List<Rule> rules;
    private Map<Integer, Integer> labels;   // mark label line
    private List<Integer> gotos;

    J2c2Runtime(String[] codes, Map<Integer, Func.FuncValue> valuesMap) {
        this.stack = new Stack<>();
        this.result = new ArrayList<>();
        this.codes = codes;
        this.vm = valuesMap;
        this.labels = new HashMap<>();
        this.gotos = new ArrayList<>();

        initRules();
    }

    String run() {
        for(int i=0; i<codes.length; i++) {
            for(Rule r:rules) {
                if(r.check(codes[i])) {
                    Rule.Result rs = r.getCode(codes[i]);
                    labelChecker(codes[i], this.result.size());
                    if(rs.haveResult) {
                        this.result.add(rs.result);
                        gotoChecker(rs.result);
                    }
                    break;
                }
            }
        }

        addLabel();

        String rs = "";
        for(String r:this.result)
            rs += r;

        return rs;
    }

    private void labelChecker(String code, int index) {
        Pattern p = Pattern.compile("(?<=)(\\d)+(?=:)");
        Matcher m = p.matcher(code);
        if(m.find()) {
            this.labels.put(new Integer(m.group()), index);
        }
    }

    private void gotoChecker(String result) {
        if(result.contains("goto")) {
            Pattern p = Pattern.compile("(?<=goto\\sL)(\\w)+(?=;)");
            Matcher m = p.matcher(result);
            if(m.find()) {
                this.gotos.add(new Integer(m.group()));
            }
        }
    }

    private void addLabel() {
        for(Integer i:this.gotos) {
            Integer key = this.labels.get(i);
            this.result.add(key, "L" + i + ":\n");

            boolean flag = false;
            for(Map.Entry<Integer, Integer> entry:this.labels.entrySet()) {
                if(entry.getKey().intValue() == i.intValue()) {
                    flag = true;
                }
                if(flag) {
                    this.labels.replace(entry.getKey(), entry.getValue(), entry.getValue() + 1);
                }
            }
        }
    }

    private void initRules() {
        rules = new ArrayList<>();

        rules.add(new Rule("\\wconst", "#1_push", 1));
        rules.add(new Rule("\\wstore", "pop_A:_A:name=%1;\n_print", 1));
        rules.add(new Rule("\\wload", "A:_A:name_push", 1));
        rules.add(new Rule("arraylength", "pop_%1size_push", 0));
        rules.add(new Rule("if_\\wcmpge", "pop2_if(%2>=%1)\n{goto L#1;}\n_print", 1));
        rules.add(new Rule("\\wadd", "pop2_%2+%1_push", 0));
        rules.add(new Rule("if_\\wcmple", "pop2_if(%2<=%1)\n{goto L#1;}\n_print", 1));
        rules.add(new Rule("\\waload", "pop2_%2[%1]_push", 0));
        rules.add(new Rule("\\wastore", "pop_pop2_%3[%2]=%1;\n_print", 0));
        rules.add(new Rule("\\winc", "A:_A:name+=#2;\n_print", 2));
        rules.add(new Rule("goto", "goto L#1;\n_print", 1));
        rules.add(new Rule("\\wreturn", "pop_return %1;\n_print", 1));
    }

    private class Rule {
        String ruleReg;
        String[] actions;
        int argc;

        Rule(String ruleReg, String action, int argc) {
            this.ruleReg = ruleReg;
            this.actions = action.split("_");
            this.argc = argc;
        }

        boolean check(String code) {
            Pattern pat = Pattern.compile("\\d+:\\s+" + ruleReg);
            Matcher ma = pat.matcher(code);

            return ma.find();
        }

        Result getCode(String code) {
            String[] argv = new String[0];
            Argv argvA = null;
            boolean haveResult = false;
            String result = "";
            List<String> spop = new ArrayList<>();

            // get argv
            {
                String[] t1 = code.split("\\s+");
                if(argc == 1) {
                    String[] t2 = t1[1].split("_");
                    if(t1.length == 2) {
                        argv = t2;
                    }else {
                        argv = new String[t1.length - 1];
                        System.arraycopy(t1, 1, argv, 0, t1.length - 1);
                    }

                }else if(argc == 2){
                    t1[2] = t1[2].replace(",", "");
                    argv = new String[t1.length - 1];
                    System.arraycopy(t1, 1, argv, 0, argv.length);
                }
            }

            for(String action:actions) {
                switch (action){
                    case "pop":
                        spop.add(pop());
                        break;
                    case "pop2":
                        spop.add(pop());
                        spop.add(pop());
                        break;
                    case "A:":
                        Func.FuncValue fv = vm.get(new Integer(argv[1]));
                        if(fv != null) {
                            if(!fv.defined) {
                                // define var
                                result += fv.kind + " " + fv.name + ";\n";
                                haveResult = true;

                                fv.defined = true;
                                vm.replace(new Integer(argv[1]), fv);
                            }
                            argvA = new Argv(fv.name);
                        }
                        break;
                    case "push":
                        push(result);
                        result = "";
                        break;
                    case "print":
                        haveResult = true;
                        break;
                    default:
                        // replace
                        result += action;
                        if(argvA != null) {
                            result = result.replace("A:name", argvA.name)
                                    .replace("A:value", argvA.value);
                        }
                        if(argv.length > 1) {
                            result = result.replace("#1", argv[1]);
                        }
                        if(argv.length > 2) {
                            result = result.replace("#2", argv[2]);
                        }

                        for(int i=0; i<spop.size(); i++) {
                            result = result.replace("%" + (i + 1), spop.get(i));
                        }

                        if(result.matches(".*elementTypeof\\(\\w+\\).*")) {
                            Pattern p = Pattern.compile("(?<=(elementTypeof\\())(\\w)+(?=\\))");
                            Matcher m = p.matcher(result);
                            if(m.find()) {
                                String name = m.group();
                                String kind = "";

                                for(Func.FuncValue v:vm.values()) {
                                    if(v.name.equals(name)) {
                                        kind = v.kind.replace("[", "");
                                        break;
                                    }
                                }

                                result = result.replaceAll("elementTypeof\\(\\w+\\)", kind);
                            }

                        }
                        break;
                }
            }

            return new Result(haveResult, result);
        }

        /*Result getCode(String code) {
            String[] argv = new String[0];
            Argv argvA = null;
            Argv argvB = null;
            boolean haveResult = false;
            String result = "";
            String sp1 = null;
            String sp2 = null;

            //System.out.println("Code is: " + code);

            // get argv
            {
                String[] t1 = code.split("\\s+");
                if(argc == 1) {
                    String[] t2 = t1[1].split("_");
                    argv = t2;
                }else if(argc == 2){
                    String[] t2 = t1[1].split("_");
                    String[] t3 = t1[2].split(",");
                    argv = new String[t2.length + t3.length];
                    System.arraycopy(t2, 0, argv, 0, t2.length);
                    System.arraycopy(t3, 0, argv, t2.length, t3.length);
                }
            }

            // check argv
            short actionFlag = 0; //-2 pop two, -1 pop, 0 nothing, 1 push, 2 push two
            boolean printFlag = false;
            for(String action:actions) {
                if(action.contains("push")) {
                    if(action.contains("2")) {
                        actionFlag = 2;
                    }else {
                        actionFlag = 1;
                    }
                }else if(action.contains("pop")) {
                    if(action.contains("2")) {
                        actionFlag = -2;
                    }else {
                        actionFlag = -1;
                    }
                }

                if(action.contains("print")) {
                    printFlag = true;
                }else if(!printFlag) {
                    int it = 0;
                    if (action.contains("A:")) {
                        if (argvA == null) {
                            it = 1;
                        }
                    }
                    if (action.contains("B:")) {
                        if (argvB == null) {
                            it = 2;
                        }
                    }

                    if(it != 0) {
                        Func.FuncValue fv = vm.get(new Integer(argv[it]));
                        if(fv != null) {
                            if(!fv.defined) {
                                haveResult = true;
                                result += fv.kind + " " + fv.name + ";\n";
                                fv.defined = true;
                                vm.replace(new Integer(argv[it]), fv);
                            }
                            if(it == 1) {
                                argvA = new Argv(fv.name);
                            }else {
                                argvB = new Argv(fv.name);
                            }
                        }
                    }
                }else {
                    result += action;
                    haveResult = true;
                }
            }

            // stack action
            switch (actionFlag + 2) {
                case 0:
                    break;
                case 1:
                    if(argvA != null) {
                        argvA.value = stack.pop();
                    }else {
                        sp1 = stack.pop();
                    }
                    break;
                case 2:
                    break;
                case 3:
                    if(argvA != null) {
                        stack.push(argvA.name);
                    }else {
                        stack.push(argv[1]);
                    }
                    break;
                case 4:
                    if(argvA != null) {
                        stack.push(argvA.name);
                    }else {
                        stack.push(argv[1]);
                    }
                    if(argvB != null) {
                        stack.push(argvB.name);
                    }else {
                        stack.push(argv[2]);
                    }
            }

            if(printFlag) {
                if (argvA != null) {
                    result = result.replace("A:name", argvA.name)
                            .replace("A:value", argvA.value);
                }
                if (argvB != null) {
                    result = result.replace("B:name", argvB.name)
                            .replace("B:value", argvB.value);
                }
                if(sp1 != null) {
                    result = result.replace("#1", sp1);
                }
                result += ";\n";
            }
            //showStack();


            return new Result(haveResult, result);
        }*/

        private String pop() {
            return stack.pop();
        }

        private String[] pop2() {
            String[] rs = new String[2];
            rs[0] = stack.pop();
            rs[1] = stack.pop();
            return rs;
        }

        private void push(String a) {
            stack.push(a);
        }

        private void push2(String a, String b) {
            stack.push(a);
            stack.push(b);
        }

        private void showStack() {
            List<String> ts = new ArrayList<>();
            for(String s:stack) {
                ts.add(0, s);
            }

            System.out.println("Top.");
            System.out.println("_______");
            for(String s:ts) {
                System.out.println(s);
            }
            System.out.println("_______");
            System.out.println("Bottom.");
        }

        class Argv {
            String name;
            String value;

            public Argv(String name) {
                this.name = name;
                this.value = "";
            }

            public Argv(String name, String value) {
                this.name = name;
                this.value = value;
            }
        }

        class Result {
            boolean haveResult;
            String result;

            Result(boolean haveResult, String result) {
                this.haveResult = haveResult;
                this.result = result;
            }
        }
    }
}

