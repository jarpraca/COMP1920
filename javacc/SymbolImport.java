public class SymbolImport extends Symbol {

    private Boolean isStatic;
    
    public SymbolImport(String type, String name, Boolean isStatic) {
        super(type, name);
        this.isStatic = isStatic;
    }

    public Boolean isStatic() {
        return this.isStatic;
    }

}