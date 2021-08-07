import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class VMtranslator {
    public VMtranslator(){
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

        List<Path> vmFiles = new ArrayList<>();
        // 引数が、ディレクトリの場合とファイルの場合とがある
        if(Files.isDirectory(source)){ // ディレクトリを指定された場合は、その中の全てのvmファイルをListに入れる
            try {
                DirectoryStream<Path> stream = Files.newDirectoryStream(source);
                for (Path file: stream) {
                    if(file.getFileName().toString().endsWith(".vm")){
                        vmFiles.add(file);
                    }
                }    
            } catch (IOException e) {
            }
        }
        else{ // ファイルを指定された場合は、vmファイルであればListに入れる
            if(!source.toString().endsWith(".vm")){
                System.err.println("Specified file is not VM file");
                System.exit(1);
            }
            vmFiles.add(source);
        }

        // 出力ファイル名（xxx.asm の xxx）を決める
        Path target;
        if(Files.isDirectory(source)){ // ディレクトリを指定された場合は、ディレクトリ名をファイル名にする
            target = Paths.get(source + "/" + source.getFileName() + ".asm");
            System.out.println(target.toString());
        }
        else{ // ファイルを指定された場合は、そのファイル名と同じにする
            target = Paths.get(source.toString().substring(0, source.toString().lastIndexOf(".")) + ".asm");
            System.out.println(target.toString());
        }
        CodeWriter writer = new CodeWriter(target);

        // ブーストストラップコード（初期化処理）
        writer.writeInit();

        for(Path sourceFile: vmFiles){
            Parser parser = new Parser(sourceFile);
            String commandType;
            while(parser.hasMoreCommands()){
                commandType = parser.commandType();
                switch(commandType){
                    case "C_PUSH":
                    case "C_POP":
                        writer.writePushPop(commandType, parser.arg1(), Integer.parseInt(parser.arg2()));
                        break;
                    case "C_ARITHMETIC":
                        writer.writeArithmetic(parser.arg1());
                        break;
                    case "C_LABEL":
                        writer.writeLabel(parser.arg1());
                        break;
                    case "C_IF":
                        writer.writeIf(parser.arg1());
                        break;
                    case "C_GOTO":
                        writer.writeGoto(parser.arg1());
                        break;
                    case "C_FUNCTION":
                        writer.writeFunction(parser.arg1(), Integer.parseInt(parser.arg2()));
                        break;
                    case "C_RETURN":
                        writer.writeReturn();
                        break;
                    case "C_CALL":
                        writer.writeCall(parser.arg1(), Integer.parseInt(parser.arg2()));
                        break;
                    default:
                        break;
                }
                parser.advance();
            }
        }
        writer.close();
    }
}