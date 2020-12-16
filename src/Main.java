import java.io.FileNotFoundException;
import java.util.ArrayList;

public class Main {
    public static void main(String args[]) throws ParseException, FileNotFoundException, Exception {
        SyntaxAnalysis syntaxTree = new SyntaxAnalysis(new java.io.FileInputStream(args[0]));
        SimpleNode root = syntaxTree.Program(); // returns reference to root node
        root.dump(""); // prints the tree on the screen
        System.out.println("");
        
        if (syntaxTree.getCount() > 0)
            throw new ParseException(syntaxTree.getCount() + " errors encountered");

        SemanticAnalysis semantic = new SemanticAnalysis();

        semantic.createSymbolTable(root, new SymbolTable());
        
        if(args.length > 1 && args[1].equals("-p"))
            semantic.printSymbolTables();
        
        semantic.semanticAnalysis(root);
        semantic.printErrors();
        
        String[] file = args[0].split("\\\\");
        String[] filename = file[file.length - 1].split("\\.");
        CodeWriter codeWriter = new CodeWriter(filename[0], semantic);

        codeWriter.generateCode(root, semantic.getGlobalTable(), "program");
    }
}
