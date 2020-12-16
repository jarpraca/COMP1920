import java.util.HashMap;
import java.util.ArrayList;
import java.util.*;

public class SymbolTable {

    private SymbolVar symbolReturn;
    private HashMap<String, Symbol> table;
    private ArrayList<AbstractMap.SimpleEntry<String, Symbol>> params;
    private SymbolTable parentTable;

    public SymbolTable() {}

    public SymbolTable(SymbolTable parentTable) {
        this.table = new HashMap<String, Symbol>();
        this.params = new ArrayList<AbstractMap.SimpleEntry<String, Symbol>>();
        this.parentTable = parentTable;
    }
    
    public HashMap<String, Symbol> getTable() {
        return this.table;
    }

    public SymbolTable getParent() {
        return this.parentTable;
    }

    public Symbol get(String key) {
        return this.table.get(key);
    }

    public void remove(String key) {
        this.table.remove(key);
    }

    public boolean add(String key, Symbol value) {
        return this.table.putIfAbsent(key, value) == null;
    }

    public boolean hasKey(String key) {
        return this.table.containsKey(key);
    }

    public boolean hasParam(String key) {
        for(AbstractMap.SimpleEntry<String, Symbol> param : this.params){
            if(key.equals(param.getKey()))
                return true;
        }
        return false;
    }

    public ArrayList<AbstractMap.SimpleEntry<String, Symbol>> getParams() {
        return params;
    }

    public Symbol getReturn() {
        return symbolReturn;
    }

    public void setReturn(SymbolVar ret) {
        symbolReturn = ret;
    }

    public boolean addParam(String key, Symbol param) {
        if(this.table.putIfAbsent(key, param) == null){
            this.params.add(new AbstractMap.SimpleEntry<String, Symbol>(key, param));
            return true;
        }
        return false;
    }

}