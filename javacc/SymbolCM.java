public class SymbolCM extends Symbol {

    private SymbolTable symbolTable;
    
    public SymbolCM(String type, String name, SymbolTable table) {
        super(type, name);
        this.symbolTable = table;
    }

    public SymbolTable getSymbolTable() {
        return this.symbolTable;
    }

}