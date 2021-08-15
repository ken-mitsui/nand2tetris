import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class JackAnalyzer {
    public JackAnalyzer(){
    }

    private static void writeFile(Path path, List<String> outputList){
        System.out.println(path);
        // 既にファイルがある場合は一旦削除してから生成する
        try {
            Files.deleteIfExists(path);
            Files.createFile(path);
            Files.write(path, outputList, StandardOpenOption.WRITE);
        } catch (IOException e) {
            System.err.println("File cannot be deleted or created");
            System.exit(1);
        }
    }

    public static void main(String args[]){
        if(args.length == 0){
            System.err.println("No file or directory is specified");
            System.exit(1);
        }
        Path source = Paths.get(args[0]);
        if(!Files.exists(source)){
            System.err.println("Specified file or directory is not found");
            System.exit(1);
        }

        List<Path> jackFiles = new ArrayList<>();
        // 引数が、ディレクトリの場合とファイルの場合とがある
        if(Files.isDirectory(source)){ // ディレクトリを指定された場合は、その中の全てのjackファイルをListに入れる
            try {
                DirectoryStream<Path> stream = Files.newDirectoryStream(source);
                for (Path file: stream) {
                    if(file.getFileName().toString().endsWith(".jack")){
                        jackFiles.add(file);
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
            jackFiles.add(source);
        }

        Path tokenizedFile;
        JackTokenizer tokenizer;
        String tokenType = "";
        String tag = "";
        String element = "";
        List<String> outputList = new ArrayList<>();
        CompilationEngine engine;

        for(Path sourceFile: jackFiles){
            // JackTokenizer を使って xxxT.xml ファイルを生成する
            tokenizedFile = Paths.get(sourceFile.toString().substring(0, sourceFile.toString().lastIndexOf(".")) + "T.xml");
            tokenizer = new JackTokenizer(sourceFile);
            outputList.clear();
            outputList.add("<tokens>");
            while(tokenizer.hasMoreCommands()){
                tokenizer.advance();
                tokenType = tokenizer.tokenType();
                switch(tokenType){
                    case "KEYWORD":
                        tag = "keyword";
                        element = tokenizer.keyWord().toLowerCase();
                        break;
                    case "SYMBOL":
                        tag = "symbol";
                        element = tokenizer.symbol();
                        String str = element;
                        switch(str){
                            case "<":
                                element = "&lt;";
                                break;
                            case ">":
                                element = "&gt;";
                                break;
                            case "&":
                                element = "&amp;";
                                break;
                        }
                        break;
                    case "IDENTIFIER":
                        tag = "identifier";
                        element = tokenizer.identifier();
                        break;
                    case "INT_CONST":
                        tag = "integerConstant";
                        element = Integer.valueOf(tokenizer.intVal()).toString();
                        break;
                    case "STRING_CONST":
                        tag = "stringConstant";
                        element = tokenizer.stringVal();
                        break;
                }
                outputList.add("<" + tag + "> " + element + " </" + tag + ">");
            }
            outputList.add("</tokens>");

            // トークナイザ
            writeFile(tokenizedFile, outputList);
            // パーサ
            engine = new CompilationEngine(sourceFile);
            engine.compileClass();
        }
    }
}