import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CompilationEngine {
    private Path outputFile;
    private List<String> outputList = new ArrayList<>();
    private JackTokenizer tokenizer;
    private String tokenType, keyWord, symbol, identifier, stringVal;
    private int intVal;
    private final List<String> CLASS_VAR_DEC = Arrays.asList("STATIC", "FIELD");
    private final List<String> SUBROUTINE_DEC = Arrays.asList("CONSTRUCTOR", "FUNCTION", "METHOD");
    private final List<String> OP = Arrays.asList("+", "-", "*", "/", "&", "|", "<", ">", "=");
    private final List<String> UNARY_OP = Arrays.asList("-", "~");
    private final List<String> KEYWORD_CONSTANT = Arrays.asList("TRUE", "FALSE", "NULL", "THIS");
    private enum TYPE {
        KEYWORD,
        SYMBOL,
        IDENTIFIER,
        INT_CONST,
        STRING_CONST
    }
    
    public CompilationEngine(Path source){
        if(!Files.exists(source)){
            System.err.println("Specified file or directory is not found");
            System.exit(1);
        }
        List<Path> sourceFiles = new ArrayList<>();
        // 引数が、ディレクトリの場合とファイルの場合とがある
        if(Files.isDirectory(source)){ // ディレクトリを指定された場合は、その中の全てのjackファイルをListに入れる
            try {
                DirectoryStream<Path> stream = Files.newDirectoryStream(source);
                for (Path file: stream) {
                    if(file.getFileName().toString().endsWith(".jack")){
                        sourceFiles.add(file);
                    }
                }    
            } catch (IOException e) {
            }
        }
        else{ // ファイルを指定された場合は、jackファイルであればListに入れる
            if(!source.toString().endsWith(".jack")){
                System.err.println("Specified file is not Jack file");
                System.exit(1);
            }
            sourceFiles.add(source);
        }

        for(Path sourceFile : sourceFiles){
            // JackTokenizer を使って入力ファイルを読み込む
            tokenizer = new JackTokenizer(sourceFile);
            // 出力ファイルの準備をする
            outputFile = Paths.get(sourceFile.toString().substring(0, sourceFile.toString().lastIndexOf(".")) + ".xml");
            System.out.println(outputFile.toString());
            outputList.clear();
        }
    }

    public void compileClass(){
        outputList.add("<class>");
        // class
        goNextToken();
        addSentence(TYPE.KEYWORD, "CLASS");
        // className
        goNextToken();
        addSentence(TYPE.IDENTIFIER);
        // "{"
        goNextToken();
        addSentence(TYPE.SYMBOL, "{");
        // classVarDec*
        goNextToken();
        while(isTypeSame(TYPE.KEYWORD)){
            keyWord = tokenizer.keyWord();
            if(!CLASS_VAR_DEC.contains(keyWord)){
                break;
            }
            compileClassVarDec();
        }
        // subroutineDec*
        while(isTypeSame(TYPE.KEYWORD)){
            keyWord = tokenizer.keyWord();
            if(!SUBROUTINE_DEC.contains(keyWord)){
                break;
            }
            compileSubroutine();
        }
        // "}"
        addSentence(TYPE.SYMBOL, "}");
        outputList.add("</class>");

        // ファイルを出力。既にファイルがある場合は一旦削除してから生成する
        try {
            Files.deleteIfExists(outputFile);
            Files.createFile(outputFile);
            Files.write(outputFile, outputList, StandardOpenOption.WRITE);
        } catch (IOException e) {
            System.err.println("File cannot be deleted or created");
            System.exit(1);
        }
        
    }

    public void compileClassVarDec(){
        outputList.add("<classVarDec>");
        // static or field
        addSentence(TYPE.KEYWORD);
        // type。keyword（intなど）の場合と、identifier（className）の場合とがある
        goNextToken();
        if(!addSentence(TYPE.KEYWORD)){
            addSentence(TYPE.IDENTIFIER);
        }
        // varName (, varName)*
        goNextToken();
        while(true){ // varName が複数ある場合は繰り返す
            addSentence(TYPE.IDENTIFIER);
            // ";" or ","
            goNextToken();
            addSentence(TYPE.SYMBOL);
            if(";".equals(symbol)){
                break;
            }
            else if(",".equals(symbol)){
                goNextToken();
            }
        }
        outputList.add("</classVarDec>");
        goNextToken();
    }

    public void compileSubroutine(){
        outputList.add("<subroutineDec>");
        // constructor or function or method
        addSentence(TYPE.KEYWORD);
        // void or type。keyword（intなど）の場合と、identifier（className）の場合とがある
        goNextToken();
        if(!addSentence(TYPE.KEYWORD)){
            addSentence(TYPE.IDENTIFIER);
        }
        // subroutineName
        goNextToken();
        addSentence(TYPE.IDENTIFIER);
        // "("
        goNextToken();
        addSentence(TYPE.SYMBOL, "(");
        // parameterList
        goNextToken();
        //if(!isTypeSame(TYPE.SYMBOL)){
            compileParameterList();
        //}
        // ")"
        addSentence(TYPE.SYMBOL, ")");
        goNextToken();
        // subroutineBody
        outputList.add("<subroutineBody>");
        // "{"
        addSentence(TYPE.SYMBOL, "{");
        // varDec*
        goNextToken();
        while(isTypeSame(TYPE.KEYWORD)){
            if(!"VAR".equals(tokenizer.keyWord())){
                break;
            }
            compileVarDec();
        }
        // statements
        if(!isTypeSame(TYPE.SYMBOL)){
            compileStatements();
        }
        // "}"
        addSentence(TYPE.SYMBOL, "}");
        outputList.add("</subroutineBody>");
        outputList.add("</subroutineDec>");
        goNextToken();
    }

    public void compileParameterList(){
        outputList.add("<parameterList>");
        while(!isTypeSame(TYPE.SYMBOL)){
            // type。keyword（intなど）の場合と、identifier（className）の場合とがある
            if(!addSentence(TYPE.KEYWORD)){
                addSentence(TYPE.IDENTIFIER);
            }
            // varName
            goNextToken();
            addSentence(TYPE.IDENTIFIER);
            // "," (登場は0回か1回)
            goNextToken();
            if(addSentence(TYPE.SYMBOL, ",")){
                goNextToken();
            }
            else{
                break;
            }
        }
        outputList.add("</parameterList>");
    }

    public void compileVarDec(){
        outputList.add("<varDec>");
        // "var"
        addSentence(TYPE.KEYWORD, "VAR");
        // type。keyword（intなど）の場合と、identifier（className）の場合とがある
        goNextToken();
        if(!addSentence(TYPE.KEYWORD)){
            addSentence(TYPE.IDENTIFIER);
        }
        // varName (, varName)*
        goNextToken();
        while(true){
            addSentence(TYPE.IDENTIFIER);
            // "," or ";"
            goNextToken();
            if(addSentence(TYPE.SYMBOL, ",")){
                goNextToken();
            }
            else{
                addSentence(TYPE.SYMBOL, ";");
                goNextToken();
                break;
            }
        }
        outputList.add("</varDec>");
    }

    public void compileStatements(){
        outputList.add("<statements>");
        while(isTypeSame(TYPE.KEYWORD)){
            keyWord = tokenizer.keyWord();
            switch(keyWord){
                case "LET":
                    compileLet();
                    break;
                case "IF":
                    compileIf();
                    break;
                case "WHILE":
                    compileWhile();
                    break;
                case "DO":
                    compileDo();
                    break;
                case "RETURN":
                    compileReturn();
                    break;
            }
            //goNextToken();
        }
        outputList.add("</statements>");
    }

    public void compileDo(){
        outputList.add("<doStatement>");
        // "do"
        addSentence(TYPE.KEYWORD, "DO");
        // subroutineCall
        // 始まりが "className ." の場合 と "subroutineName (" の場合とがある。いずれにしろ最初のトークンは identifier
        // className or subroutineName
        goNextToken();
        addSentence(TYPE.IDENTIFIER);
        // "." (classNameの場合のみ)があるかどうか
        goNextToken();
        if(addSentence(TYPE.SYMBOL, ".")){
            // subroutineName
            goNextToken();
            addSentence(TYPE.IDENTIFIER);
            goNextToken();
        }
        // "("
        addSentence(TYPE.SYMBOL, "(");
        // expressionList
        goNextToken();
        compileExpressionList();
        // ")"
        addSentence(TYPE.SYMBOL, ")");
        // ";"
        goNextToken();
        addSentence(TYPE.SYMBOL, ";");
        outputList.add("</doStatement>");
        goNextToken();
    }

    public void compileLet(){
        outputList.add("<letStatement>");
        // "let"
        addSentence(TYPE.KEYWORD, "LET");
        // varName
        goNextToken();
        addSentence(TYPE.IDENTIFIER);
        // "[expression]" (登場は0回か1回)
        goNextToken();
        if(addSentence(TYPE.SYMBOL, "[")){
            // expression
            goNextToken();
            compileExpression();
            // "]"
            addSentence(TYPE.SYMBOL, "]");
            goNextToken();
        }
        // "="
        addSentence(TYPE.SYMBOL, "=");
        // expression
        goNextToken();
        compileExpression();
        // ";"
        addSentence(TYPE.SYMBOL, ";");
        outputList.add("</letStatement>");
        goNextToken();
    }

    public void compileWhile(){
        outputList.add("<whileStatement>");
        // "while"
        addSentence(TYPE.KEYWORD, "WHILE");
        // "("
        goNextToken();
        addSentence(TYPE.SYMBOL, "(");
        // expression
        goNextToken();
        compileExpression();
        // ")"
        addSentence(TYPE.SYMBOL, ")");
        // "{"
        goNextToken();
        addSentence(TYPE.SYMBOL, "{");
        // statements
        goNextToken();
        compileStatements();
        // "}"
        addSentence(TYPE.SYMBOL, "}");
        outputList.add("</whileStatement>");
        goNextToken();
    }

    public void compileReturn(){
        outputList.add("<returnStatement>");
        // "return"
        addSentence(TYPE.KEYWORD, "RETURN");
        // expression (登場は0回か1回)
        goNextToken();

        if(!addSentence(TYPE.SYMBOL, ";")){ // ";"
            compileExpression();
            addSentence(TYPE.SYMBOL, ";");
        }
        outputList.add("</returnStatement>");
        goNextToken();
    }

    public void compileIf(){
        outputList.add("<ifStatement>");
        // "if"
        addSentence(TYPE.KEYWORD, "IF");
        // "("
        goNextToken();
        addSentence(TYPE.SYMBOL, "(");
        // expression
        goNextToken();
        compileExpression();
        // ")"
        addSentence(TYPE.SYMBOL, ")");
        // "{"
        goNextToken();
        addSentence(TYPE.SYMBOL, "{");
        // statements
        goNextToken();
        compileStatements();
        // "}"
        addSentence(TYPE.SYMBOL, "}");
        // "else" (登場は0回か1回)
        goNextToken();
        if(addSentence(TYPE.KEYWORD, "ELSE")){
            // "{"
            goNextToken();
            addSentence(TYPE.SYMBOL, "{");
            // statements
            goNextToken();
            compileStatements();
            // "}"
            addSentence(TYPE.SYMBOL, "}");
            goNextToken();
        }
        outputList.add("</ifStatement>");
    }

    public void compileExpression(){
        outputList.add("<expression>");
        // term
        compileTerm();
        // (op term)*
        while(isTypeSame(TYPE.SYMBOL)){
            symbol = tokenizer.symbol();
            if(!OP.contains(symbol)){
                break;
            }
            // op
            addSentence(TYPE.SYMBOL, symbol);
            goNextToken();
            // term
            compileTerm();
        }

        /*
        if(isTypeSame(TYPE.SYMBOL)){
            symbol = tokenizer.symbol();
            if(OP.contains(symbol)){
                addSentence(TYPE.SYMBOL, symbol);
                goNextToken();
                // term
                compileTerm();
            }
        }
        */
        outputList.add("</expression>");
    }

    public void compileTerm(){
        outputList.add("<term>");
        // integerConstant
        if(addSentence(TYPE.INT_CONST)){
            goNextToken();
        }
        // stringConstant
        else if(addSentence(TYPE.STRING_CONST)){
            goNextToken();
        }
        // keywordConstant
        else if(isTypeSame(TYPE.KEYWORD)){
            keyWord = tokenizer.keyWord();
            if(KEYWORD_CONSTANT.contains(keyWord)){
                addSentence(TYPE.KEYWORD, keyWord);
                goNextToken();
            }
        }
        // "( expression )" or "unaryOp term"
        else if(isTypeSame(TYPE.SYMBOL)){
            // "( expression )"
            if(addSentence(TYPE.SYMBOL, "(")){
                // expression
                goNextToken();
                compileExpression();
                // ")"
                addSentence(TYPE.SYMBOL, ")");
                goNextToken();
            }
            // "unaryOp term"
            else{
                symbol = tokenizer.symbol();
                if(UNARY_OP.contains(symbol)){
                    // unaryOp
                    addSentence(TYPE.SYMBOL, symbol);
                    // term
                    goNextToken();
                    compileTerm();
                }
            }
        }
        // varName or "varName [ expression ]" or subroutineCall
        else if(addSentence(TYPE.IDENTIFIER)){
            goNextToken();
            // "varName [ expression ]"
            if(addSentence(TYPE.SYMBOL, "[")){ // "["
                // expression
                goNextToken();
                compileExpression();
                // "]"
                addSentence(TYPE.SYMBOL, "]");
                goNextToken();
            }
            // subroutineCall のうち、"subroutineName (" の場合
            else if(addSentence(TYPE.SYMBOL, "(")){ // "("
                // expressionList
                goNextToken();
                compileExpressionList();
                // ")"
                addSentence(TYPE.SYMBOL, ")");
                goNextToken();
            }
            // subroutineCall のうち、"className ." の場合
            else if(addSentence(TYPE.SYMBOL, ".")){ // "."
                // subroutineName
                goNextToken();
                addSentence(TYPE.IDENTIFIER);
                // "("
                goNextToken();
                addSentence(TYPE.SYMBOL, "(");
                // expressionList
                goNextToken();
                compileExpressionList();
                // ")"
                addSentence(TYPE.SYMBOL, ")");
                goNextToken();
            }
            // varName
            else{
            }
        }
        outputList.add("</term>");    
    }

    public void compileExpressionList(){
        outputList.add("<expressionList>");
        // expression 無し
        if(isTypeSame(TYPE.SYMBOL) && ")".equals(tokenizer.symbol())){
            outputList.add("</expressionList>");
            return;
        }
        // expression (1つ以上)
        while(true){
            // expression
            compileExpression();
            // ","
            if(!addSentence(TYPE.SYMBOL, ",")){
                break;
            }
            else{
                goNextToken();
            }
        }
        outputList.add("</expressionList>");
    }

    private String getSentence(TYPE type, String token){
        String tag = "";
        String element = token;
        switch(type){
            case KEYWORD:
                tag = "keyword";
                element = token.toLowerCase();
                break;
            case SYMBOL:
                tag = "symbol";
                break;
            case IDENTIFIER:
                tag = "identifier";
                break;
            case INT_CONST:
                tag = "integerConstant";
                break;
            case STRING_CONST:
                tag = "stringConstant";
                break;
        }
        return "<" + tag + "> " + element + " </" + tag + ">";
    }

    private void goNextToken(){
        tokenizer.advance();
        tokenType = tokenizer.tokenType();
    }

    private boolean isTypeSame(TYPE type){
        // プログラム構造から期待されるトークン種類と、現在のトークン種類が確かに一致しているか
        return type.toString().equals(tokenType);
    }

    private boolean addSentence(TYPE type, String ... token){
        if(!isTypeSame(type)){
            return false;
        }
        switch(type){
            case KEYWORD:
                keyWord = tokenizer.keyWord();
                // プログラム構造から期待される文字列がある場合は、それと、取得したkeyWordとが一致することを確認する
                if(token.length > 0 && !token[0].equals(keyWord)){
                    return false;
                }
                outputList.add(getSentence(type, keyWord));
                break;
            case SYMBOL:
                symbol = tokenizer.symbol();
                if(token.length > 0 && !token[0].equals(symbol)){
                    return false;
                }
                String str = symbol;
                switch(symbol){
                    case "<":
                        str = "&lt;";
                        break;
                    case ">":
                        str = "&gt;";
                        break;
                    case "&":
                        str = "&amp;";
                        break;
                }
                outputList.add(getSentence(type, str));
                break;
            case IDENTIFIER:
                identifier = tokenizer.identifier();
                if(token.length > 0 && !token[0].equals(identifier)){
                    return false;
                }
                outputList.add(getSentence(type, identifier));
                break;
            case INT_CONST:
                intVal = tokenizer.intVal();
                String strVal = Integer.valueOf(intVal).toString();
                if(token.length > 0 && !token[0].equals(strVal)){
                    return false;
                }
                outputList.add(getSentence(type, strVal));
                break;
            case STRING_CONST:
                stringVal = tokenizer.stringVal();
                if(token.length > 0 && !token[0].equals(stringVal)){
                    return false;
                }
                outputList.add(getSentence(type, stringVal));
                break;
        }
        return true;
    }

    public static void main(String args[]){
    }
}