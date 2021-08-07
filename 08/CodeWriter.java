import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CodeWriter {
    private Path target;
    private List<String> outputList = new ArrayList<>();
    public static final String LS = System.getProperty("line.separator");
    private static int eqLabelCount = 0; // (EQ_TRUE_1) の数字の部分
    private static int returnAddressCount = 0; // return-address1 の数字の部分
    // Class1 で static[0],[1] を使ったら、Class2 では static[2] から使い始めるため、
    // 各functionでの開始位置を覚えておく(map)
    private static int staticCount = 0;
    private String currentFunction = "";
    private Map<String, Integer> functionMap = new HashMap<String, Integer>();

    public CodeWriter(Path targetFile){
        target = targetFile;
        System.out.println(target.toString());
        // 既にファイルがある場合は一旦削除してから生成する
        try {
            Files.deleteIfExists(target);
            Files.createFile(target);
        } catch (IOException e) {
            System.err.println("File cannot be deleted or created");
            System.exit(1);
        }
    }

    public void writeArithmetic(String command){
        outputList.add("// " + command);
        switch(command){
            case "add":
                outputList.add(commandCalcTwoValues("+"));
                return;
            case "sub":
                outputList.add(commandCalcTwoValues("-"));
                return;
            case "neg":
                outputList.add(commandCalcOneValue(true));
                return;
            case "eq":
                outputList.add(commandCompare("JEQ"));
                return;
            case "gt":
                outputList.add(commandCompare("JGT"));
                return;
            case "lt":
                outputList.add(commandCompare("JLT"));
                return;
            case "and":
                outputList.add(commandCalcTwoValues("&"));
                return;
            case "or":
                outputList.add(commandCalcTwoValues("|"));
                return;
            case "not":
                outputList.add(commandCalcOneValue(false));
                return;
            default:
                return;
        }
   }

   /*
   各コマンドでの出力例は、"AnswerStackTest.asm"を参照
   */

   private String commandCalcTwoValues(String type){
       return "@SP" + LS + "AM=M-1" + LS + "D=M" + LS + "A=A-1" + LS + "D=M" + type + "D"
       + LS + "@SP" + LS + "A=M-1" + LS + "M=D";
   }

   private String commandCalcOneValue(boolean isNegCommand){
       String str = isNegCommand ? "@0" + LS + "D=A-D" : "D=!D";
       return "@SP" + LS + "A=M-1" + LS + "D=M" + LS + str + LS + "@SP" + LS + "A=M-1" + LS + "M=D";
   }

   private String commandCompare(String type){
       eqLabelCount++;
       return "@SP" + LS + "AM=M-1" + LS + "D=M" + LS + "A=A-1" + LS + "D=M-D" + LS
       + "@EQ_TRUE_" + eqLabelCount + LS + "D;" + type + LS
       + "@0" + LS + "D=A" + LS + "@SP" + LS + "A=M-1" + LS + "M=D" + LS
       + "@EQ_FALSE_" + eqLabelCount + LS + "0;JMP" + LS
       + "(EQ_TRUE_" + eqLabelCount + ")" + LS + "@1" + LS + "D=A" + LS + "@0" + LS + "D=A-D" + LS
       + "@SP" + LS + "A=M-1" + LS + "M=D"  + LS + "(EQ_FALSE_" + eqLabelCount + ")";
   }

   public void writePushPop(String command, String segment, int index){
       switch(command){
            case "C_PUSH":
            outputList.add("// push " + segment + " " + index);
                switch(segment){
                    case "argument":
                        outputList.add(pushOther("ARG", index));
                        return;
                    case "local":
                        outputList.add(pushOther("LCL", index));
                        return;
                    case "static":
                        outputList.add(pushStatic(index));
                        return;
                    case "constant":
                        outputList.add(pushConstant(index));
                        return;
                    case "this":
                        outputList.add(pushOther("THIS", index));
                        return;
                    case "that":
                        outputList.add(pushOther("THAT", index));
                        return;
                    case "pointer":
                        outputList.add(pushPointer(index==0));
                        return;
                    case "temp":
                        outputList.add(pushOther("R5", index));
                        return;
                }
                return;
            case "C_POP":
            outputList.add("// pop " + segment + " " + index);
                switch(segment){
                    case "argument":
                        outputList.add(popOther("ARG", index));
                        return;
                    case "local":
                        outputList.add(popOther("LCL", index));
                        return;
                    case "static":
                        outputList.add(popStatic(index));
                        return;
                    case "this":
                        outputList.add(popOther("THIS", index));
                        return;
                    case "that":
                        outputList.add(popOther("THAT", index));
                        return;
                    case "pointer":
                        outputList.add(popPointer(index==0));
                        return;
                    case "temp":
                        outputList.add(popOther("R5", index));
                        return;
                }
                return;
            default:
                return;
        }
    }

    private String pushConstant(int index){
        return "@" + index + LS + "D=A" + LS + "@SP" + LS + "A=M"
        + LS + "M=D" + LS + "@SP" + LS + "M=M+1";
    }

    private String pushPointer(boolean isThis){
        String str = isThis ? "@THIS" : "@THAT";
        return str + LS + "D=M" + LS + "@SP" + LS + "A=M" + LS
        + "M=D" + LS + "@SP" + LS + "M=M+1";
    }

    private String pushStatic(int index){
        int start = functionMap.containsKey(currentFunction) ?
        functionMap.get(currentFunction) : 0;
        return "@StaticTest." + (index + start) + LS + "D=M" + LS + "@SP" + LS
        + "A=M" + LS + "M=D" + LS + "@SP" + LS + "M=M+1";
    }

    private String pushOther(String type, int index){
        String str = "R5".equals(type) ? "D=A" : "D=M";
        return "@" + type + LS + str + LS + "@" + index + LS + "D=D+A" + LS
        + "A=D" + LS + "D=M" + LS + "@SP" + LS + "A=M" + LS + "M=D" + LS
        + "@SP" + LS + "M=M+1";
    }

    private String popPointer(boolean isThis){
        String str = isThis ? "@THIS" : "@THAT";
        return "@SP" + LS + "AM=M-1" + LS + "D=M" + LS + str + LS + "M=D";
    }

    private String popStatic(int index){
        staticCount++;
        int start = functionMap.containsKey(currentFunction) ?
        functionMap.get(currentFunction) : 0;
        return "@SP" + LS + "AM=M-1" + LS + "D=M" + LS
        + "@StaticTest." + (index + start) + LS + "M=D";
    }

    private String popOther(String type, int index){
        String str = "R5".equals(type) ? "D=A" : "D=M";
        return "@" + type + LS + str + LS + "@" + index + LS + "D=D+A" + LS
        + "@R13" + LS + "M=D" + LS + "@SP" + LS + "AM=M-1" + LS
        + "D=M" + LS + "@R13" + LS + "A=M" + LS + "M=D";
    }

    public void writeLabel(String label){
        outputList.add("// label " + label);
        outputList.add("(" + label + ")");
    }

    public void writeIf(String label){
        outputList.add("// if-goto " + label);
        outputList.add("@SP" + LS + "M=M-1" + LS + "A=M" + LS
        + "D=M" + LS + "@" + label + LS + "D;JNE");
    }

    public void writeGoto(String label){
        outputList.add("// goto " + label);
        outputList.add("@" + label + LS + "0;JMP");
    }

    public void writeFunction(String functionName, int numLocals){
        outputList.add("// function " + functionName + " " + numLocals);
        outputList.add("(" + functionName + ")");
        for(int i=0; i<numLocals; i++){
            outputList.add(pushConstant(0));
            outputList.add("@LCL" + LS + "D=M" + LS + "@" + i + LS + "D=D+A");
        }
        currentFunction = ( functionName.indexOf(".") == -1 ) ?
        functionName : functionName.substring(0, functionName.indexOf("."));
        if(!functionMap.containsKey(currentFunction)){
            functionMap.put(currentFunction, staticCount);
        }
    }

    public void writeReturn(){
        outputList.add("// return");
        String str = "@LCL" + LS + "D=M" + LS + "@R13" + LS + "M=D" + LS
        + "@5" + LS + "D=D-A" + LS + "A=D" + LS + "D=M" + LS + "@R14" + LS + "M=D" + LS
        + "@ARG" + LS + "D=M" + LS + "@0" + LS + "D=D+A" + LS
        + "@R15" + LS + "M=D" + LS + "@SP" + LS + "AM=M-1" + LS
        + "D=M" + LS + "@R15" + LS + "A=M" + LS + "M=D" + LS
        + "@ARG" + LS + "D=M+1" + LS + "@SP" + LS + "M=D" + LS
        + "@R13" + LS + "D=M" + LS + "A=D-1" + LS + "D=M" + LS + "@THAT" + LS + "M=D" + LS
        + "@R13" + LS + "D=M" + LS + "@2" + LS + "D=D-A" + LS + "A=D" + LS
        + "D=M" + LS + "@THIS" + LS + "M=D" + LS
        + "@R13" + LS + "D=M" + LS + "@3" + LS + "D=D-A" + LS + "A=D" + LS
        + "D=M" + LS + "@ARG" + LS + "M=D" + LS
        + "@R13" + LS + "D=M" + LS + "@4" + LS + "D=D-A" + LS + "A=D" + LS
        + "D=M" + LS + "@LCL" + LS + "M=D" + LS
        + "@R14" + LS + "A=M" + LS + "0;JMP";
        outputList.add(str);
    }

    public void writeCall(String functionName, int numArgs){
        outputList.add("// call " + functionName + " " + numArgs);
        returnAddressCount++;
        String str = "@return-address" + returnAddressCount + LS
        + "D=A" + LS + "@SP" + LS + "A=M" + LS + "M=D" + LS
        + "@SP" + LS + "M=M+1" + LS
        + pushInCall("LCL") + LS
        + pushInCall("ARG") + LS
        + pushInCall("THIS") + LS
        + pushInCall("THAT") + LS
        + "@SP" + LS + "D=M" + LS + "@" + numArgs + LS
        + "D=D-A" + LS + "@5" + LS + "D=D-A" + LS
        + "@ARG" + LS + "M=D" + LS
        + "@SP" + LS + "D=M" + LS + "@LCL" + LS + "M=D" + LS;
        outputList.add(str);
        writeGoto(functionName);
        writeLabel("return-address" + returnAddressCount);
    }

    private String pushInCall(String type){
        return "@" + type + LS + "D=M" + LS + "@SP" + LS + "A=M" + LS
        + "M=D" + LS + "@SP" + LS + "M=M+1";
    }

    public void writeInit(){
        outputList.add("// init");
        outputList.add("@256" + LS + "D=A" + LS + "@SP" + LS + "M=D");
        writeCall("Sys.init", 0);
    }

    public void close(){
        // Listの内容を出力ファイルに反映する
        try {
            Files.write(target, outputList, StandardOpenOption.WRITE);
        } catch (IOException e) {
            System.err.println("File cannot be deleted or created");
            System.exit(1);
        }
    }

    public static void main(String args[]){
    }
}