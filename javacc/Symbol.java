public abstract class Symbol {

    private String type;
    private String name;
    // boolean initialized;
    
    public Symbol() {
    }

    public Symbol(String type, String name) {
        this.type = type;
        this.name = name;
    }

    // public Symbol(String type, String name, boolean initialized) {
    //     this.type = type;
    //     this.name = name;
    //     this.initialized = initialized;
    // }

    public String getName() {
        return this.name;
    }

    public String getType() {
        return this.type;
    }

    // boolean isInitialized() {
    //     return this.initialized;
    // }

    public boolean equals(Symbol s) {

        if (s.getName() == this.name && s.getType() == this.type)
            return true;

        return false;

    }

}