import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

public class JackTokenizer {
    private List<String> lines;
    private List<String> words = new ArrayList<>();
    private boolean isFirst = true;
    private final List<String> KEYWORDS = Arrays.asList(
        "class", "constructor", "function", "method", "field", "static", "var", "int", "char",
        "boolean", "void", "true", "false", "null", "this", "let", "do", "if", "else", "while", "return"
    );
    private final List<String> SYMBOLS = Arrays.asList(
        "{", "}", "(", ")", "[", "]", ".", ",", ";", "+", "-", "*", "/", "&", "|", "<", ">", "=", "~"
    );

    private String tokenType, keyWord, symbol, identifier, stringVal;
    private int intVal;
    public JackTokenizer(Path source){
        // 入力ファイルを読み込む
        try {
            lines = Files.readAllLines(source);
        } catch (IOException e) {
        System.err.println("File cannot be read");
        System.exit(1);
        }
    }

    public boolean hasMoreCommands(){
        return isFirst || words.size() > 0;
    }

    public void advance(){
        if(!hasMoreCommands()){
            return;
        }

        /*
        初回、入力ファイルを、スペースの区切りごとに分割し、words に一旦格納する
        ex: "method void run() {" -> "method", "void", "run()", "{"
        */
        if(isFirst){
            int currentNumber = 0;
            String str;
            while(currentNumber < lines.size()){
                str = lines.get(currentNumber).trim();
                // "//"で始まるコメント行や、空白行は無視し、次の行に進む
                if(str == null || str.isEmpty() || str.length() > 1 && "//".equals(str.substring(0, 2))){
                    currentNumber++;
                }
                // "/*"で始まるコメント行は、"*/"が含まれる行の次まで進む
                else if(str.length() > 1 && "/*".equals(str.substring(0, 2))){
                    while(!str.contains("*/")){
                        currentNumber++;
                        str = lines.get(currentNumber).trim();
                    }
                    currentNumber++;
                }
                // トークンが含まれる行
                else{
                    // 文の後半にコメント行があればそれを除去
                    int lastIndex = str.lastIndexOf("//");
                    if(lastIndex != -1){
                        str = str.substring(0, lastIndex);
                    }
                    // 基本的に、スペースで区切った文節に分解して words に追加していくが、""で囲まれた文字列が含まれる場合のみ例外的に扱う。
                    // "string constant" のような文字列をスペースで分割しないため
                    String[] splittedWords;
                    if(str.contains("\"")){
                        splittedWords = str.substring(0, str.indexOf("\"")).split("\s+");
                        for(String word : splittedWords){
                            words.add(word);
                        }
                        words.add(str.substring(str.indexOf("\""), str.lastIndexOf("\"") + 1));
                        splittedWords = str.substring(str.lastIndexOf("\"") + 1).split("\s+");
                        for(String word : splittedWords){
                            words.add(word);
                        }
                    }
                    else{
                        splittedWords = str.split("\s+");
                        for(String word : splittedWords){
                            words.add(word);
                        }
                    }
                    currentNumber++;
                }
            }
            isFirst = false;
        }

        /*
        words の1行目を見ていき、含まれるトークンを解析していく
        1行目に複数のトークンが含まれる場合は、その先頭のトークンを1行目に格納し、その続きのトークンは2行目以降に退避させる
        */
        final String str = words.get(0);
        // keyword のみの場合
        if(KEYWORDS.contains(str)){
            tokenType = "KEYWORD";
            keyWord = str.toUpperCase();
            words.remove(0);
            return;
        }
        // symbol のみの場合
        if(SYMBOLS.contains(str)){
            tokenType = "SYMBOL";
            symbol = str;
            words.remove(0);
            return;
        }
        // symbol が、この行のどこかに含まれるかを調べる。含まれていればその前後で分割する
        int symbolPosition = 0;
        while(symbolPosition < str.length()){
            if(SYMBOLS.contains(String.valueOf(str.charAt(symbolPosition)))){
                break;
            }
            else{
                symbolPosition++;
            }
        }
        // symbol が先頭に含まれていた場合
        if(symbolPosition == 0){
            words.add(0, String.valueOf(str.charAt(0)));
            words.set(1, str.substring(1));
            advance();
        }
        // symbol が先頭以外のどこかに含まれていた場合
        else if(symbolPosition < str.length()){
            words.set(0, str.substring(0,symbolPosition));
            words.add(1, str.substring(symbolPosition));
            advance();
        }
        // symbol が含まれない場合
        else{
            if(str.chars().allMatch(Character::isDigit)){ // 数字
                tokenType = "INT_CONST";
                intVal = Integer.parseInt(str);
            }
            else if(str.startsWith("\"")){ // ""で囲まれた文字列
                tokenType = "STRING_CONST";
                stringVal = str.substring(1, str.length() - 1);
            }
            else{ // それ以外の文字列（関数名や変数名）
                tokenType = "IDENTIFIER";
                identifier = str;
            }
            words.remove(0);
        }
    }

    public String tokenType(){
        return tokenType;
    }

    public String keyWord(){
        return keyWord;
    }

    public String symbol(){
        return symbol;
    }

    public String identifier(){
        return identifier;
    }

    public int intVal(){
        return intVal;
    }

    public String stringVal(){
        return stringVal;
    }

    public static void main(String args[]){
    }
}