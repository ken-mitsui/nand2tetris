import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class Parser {
    private List<String> lines;
    private int currentLine = 0;
    public Parser(Path source){
        try {
            lines = Files.readAllLines(source);
       } catch (IOException e) {
        System.err.println("File cannot be read");
        System.exit(1);
        }
    }

    public boolean hasMoreCommands(){
        return (currentLine < lines.size());
    }

    public void advance(){
        if(hasMoreCommands()){
            currentLine++;
        }
    }

    public String commandType(){
        String currentString = "";
        while(true){
            // 行の先頭や末尾に、空白やコメントがある場合は除去
            currentString = getTrimmedString(lines.get(currentLine));
            // 空白行やコメント行はSkip
            if(currentString.isEmpty() || "//".equals(currentString.substring(0, 2))){
                advance();
            }
            else{
                break;
            }
        }
        // コマンド行にたどり着いたので、その種類を返す。最初の2文字を見れば一意に識別可能
        switch(currentString.substring(0, 2)){
            // 算術コマンド
            case "ad": case "su": case "ne": case "eq": case "gt" :
            case "lt": case "an": case "or": case "no":
            return "C_ARITHMETIC";
            // push
            case "pu": return "C_PUSH";
            // pop
            case "po": return "C_POP";
            // label
            case "la": return "C_LABEL";
            // goto
            case "go": return "C_GOTO";
            // if-goto
            case "if": return "C_IF";
            // function
            case "fu": return "C_FUNCTION";
            // return
            case "re": return "C_RETURN";
            // call
            case "ca": return "C_CALL";
            default: return "";
        }
    }

    public String arg1(){
        String commandType  = commandType();
        String currentString = getTrimmedString(lines.get(currentLine));
        // Returnの場合はエラー
        if("C_RETURN".equals(commandType)){
            System.err.println("Not allowed to call this function");
            System.exit(1);
        }
        // 算術コマンドの場合はコマンド自体を返す
        if("C_ARITHMETIC".equals(commandType)){
            return currentString;
        }
        // それ以外のコマンドの場合は1番目の引数を返す
        int index = currentString.indexOf(" ");
        String args = currentString.substring(index + 1);
        if(args.indexOf(" ") != -1){
            return args.substring(0, args.indexOf(" "));
        }
        return args;
    }

    public String arg2(){
        String commandType  = commandType();
        String currentString = getTrimmedString(lines.get(currentLine));
        if("C_PUSH".equals(commandType) | "C_POP".equals(commandType) |
        "C_FUNCTION".equals(commandType) | "C_CALL".equals(commandType)){
            return currentString.substring(currentString.lastIndexOf(" ") + 1);
        }
        // エラー処理
        System.err.println("Not allowed to call this function");
        System.exit(1);
        return "";
    }

    private String getTrimmedString(String input){
        String output = input.trim(); // 先頭と末尾の空白がある場合は除去
        // 「D=M // D = first number」のような場合に、更に不要なコメントと空白を除去
        int index = output.indexOf("/");
        if(index != -1){
            output = output.substring(0, index).trim();
        }
        return output;
    }

    public static void main(String args[]){
    }
}