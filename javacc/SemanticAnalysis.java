import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.*;

public class SemanticAnalysis {

    private SymbolTable globalTable;
    private int errorCount;
    private ArrayList<String> errorMessages;

    public SemanticAnalysis() {
        this.errorCount = 0;
        this.errorMessages = new ArrayList<String>();
    }

    public SymbolTable getGlobalTable() {
        return this.globalTable;
    }

    public void addError(String message) {
        this.errorCount++;
        this.errorMessages.add(message);
    }

    public void printErrors() {
        if (this.errorCount > 0) {
            System.out.println(" ");
            for (String error : this.errorMessages)
                System.out.println(error);

            System.out
                    .println("\nEncountered " + this.errorCount + (this.errorCount == 1 ? " error" : " errors") + "\n");
            throw new RuntimeException();
        }
    }

    public SymbolTable createSymbolTable(SimpleNode node, SymbolTable parent) throws Exception {

        if (node instanceof ASTProgram) {
            ASTProgram program = (ASTProgram) node;
            SymbolTable table = new SymbolTable(parent);
            fillSymbolTable(node, table);
            this.globalTable = table;
            return table;

        } else if (node instanceof ASTClass) {
            SymbolTable table = new SymbolTable(parent);
            fillSymbolTable(node, table);

            return table;

        } else if (node instanceof ASTMethod) {
            ASTMethod method = (ASTMethod) node;
            SymbolTable table = new SymbolTable(parent);
            fillSymbolTable(node, table);
            return table;

        }
        return null;
    }

    public void fillSymbolTable(SimpleNode node, SymbolTable table) throws Exception {

        for (int i = 0; i < node.jjtGetNumChildren(); i++) {
            if (node.jjtGetChild(i) instanceof ASTClass) {
                String name = ((SimpleNode) node.jjtGetChild(i).jjtGetChild(0)).jjtGetName().toString();

                SymbolTable newTable = createSymbolTable((SimpleNode) node.jjtGetChild(i), table);

                SymbolCM s = new SymbolCM("class", name, newTable);
                String key = "class_" + name;
                if (!table.add(key, s))
                    this.addError(new RuntimeException(
                            "Semantic error (line " + ((SimpleNode) node.jjtGetChild(i)).jjtGetLine() + "): The class "
                                    + name + " is already declared").toString());

            } else if (node.jjtGetChild(i) instanceof ASTExtends) {
                String name = ((SimpleNode) node.jjtGetChild(i + 1)).jjtGetName().toString();

                SymbolVar s = new SymbolVar("extends", name);
                String key = "extends";

                if (!table.add(key, s))
                    this.addError(new RuntimeException("Semantic error (line "
                            + ((SimpleNode) node.jjtGetChild(i)).jjtGetLine() + "): This class already extends a class")
                                    .toString());

                SymbolTable parentTable = table.getParent();

                Boolean foundImport = false;
                for (String importKey : parentTable.getTable().keySet()) {
                    String[] words = importKey.split("_");

                    if (words[0].equals("import")) {
                        String[] className = (words[1]).split("\\.");
                   
                        if (className[0].equals(name)) {
                            foundImport = true;
                            if (className.length > 1) {
                                String[] method = className[1].split("\\(");

                                String methodKey = method[0];

                                String[] args = method[1].split(",");

                                methodKey += "_" + args.length;

                                for (int j = 0; j < args.length; j++) {
                                    if (j == args.length - 1) {
                                        String[] last = args[j].split("\\)");
                                        if(last.length > 0)
                                            args[j] = last[0];
                                        else
                                            args[j] = "";
                                    }

                                    methodKey += "_" + args[j];
                                }

                                String methodType = parentTable.get(importKey).getType();

                                SymbolCM symbolImport = new SymbolCM(methodType, method[0], null);
                                if (!table.add(methodKey, symbolImport))
                                    this.addError(new RuntimeException("Semantic error (line "
                                            + ((SimpleNode) node.jjtGetChild(i)).jjtGetLine() + "): The method "
                                            + method[0] + " is already declared with these parameters").toString());
                            }

                        }

                    }
                }

                if (!foundImport)
                    this.addError(new RuntimeException(
                            "Semantic error (line " + ((SimpleNode) node.jjtGetChild(i)).jjtGetLine()
                                    + "): The extended class was not imported").toString());
            } else if (node.jjtGetChild(i) instanceof ASTMethod) {
                SymbolTable newTable = createSymbolTable((SimpleNode) node.jjtGetChild(i), table);

                String methodType ;
                if(node.jjtGetChild(i).jjtGetChild(1).toString().equals("Identifier") || node.jjtGetChild(i).jjtGetChild(1).toString().equals("VariableType")) {
                   methodType = ((SimpleNode) node.jjtGetChild(i).jjtGetChild(1)).jjtGetName().toString();
                }
                else {
                    methodType = ((SimpleNode) node.jjtGetChild(i).jjtGetChild(1))
                        .toString();
                }

                String methodName;
                SimpleNode arguments;
                String key;

                if (!((SimpleNode) node.jjtGetChild(i).jjtGetChild(1)).toString().equals("Static_Void_Main")) {
                    SimpleNode nextNode = (SimpleNode) node.jjtGetChild(i).jjtGetChild(2);
                    int nodeIndex;

                    if (nextNode.toString() == "Array") {
                        methodType += "Array";
                        methodName = ((SimpleNode) node.jjtGetChild(i).jjtGetChild(3)).jjtGetName().toString();
                        nodeIndex = 4;
                    } else {
                        methodName = ((SimpleNode) node.jjtGetChild(i).jjtGetChild(2)).jjtGetName().toString();
                        nodeIndex = 3;
                    }

                    int argCounter = 0;
                    String argTypes = "";

                    while (((SimpleNode) node.jjtGetChild(i).jjtGetChild(nodeIndex)).toString() == "Argument") {
                        String typeName ;
                        if(node.jjtGetChild(i).jjtGetChild(nodeIndex).jjtGetChild(0).toString().equals("Identifier") || node.jjtGetChild(i).jjtGetChild(nodeIndex).jjtGetChild(0).toString().equals("VariableType")) {
                           typeName = ((SimpleNode) node.jjtGetChild(i).jjtGetChild(nodeIndex).jjtGetChild(0)).jjtGetName().toString();
                        }
                        else {
                            typeName = ((SimpleNode) node.jjtGetChild(i).jjtGetChild(nodeIndex).jjtGetChild(0))
                                .toString();
                        }
                        argTypes += "_" + typeName;

                        String array = ((SimpleNode) node.jjtGetChild(i).jjtGetChild(nodeIndex).jjtGetChild(1))
                                .toString();
                        if (array.equals("Array"))
                            argTypes += "Array";

                        argCounter++;
                        nodeIndex++;
                    }

                    key = methodName + "_" + argCounter + argTypes;

                } else {
                    methodName = "main";
                    key = "static_void_main_1";
                }

                newTable.setReturn(new SymbolVar(methodType, "return"));
                SymbolCM s = new SymbolCM(methodType, methodName, newTable);
                if (!table.add(key, s))
                    this.addError(new RuntimeException(
                            "Semantic error (line " + ((SimpleNode) node.jjtGetChild(i)).jjtGetLine() + "): The method "
                                    + methodName + " is already declared with these parameters").toString());

            } else if (node.jjtGetChild(i) instanceof ASTArgument) {

                String typeName ;
                if(node.jjtGetChild(i).jjtGetChild(0).toString().equals("Identifier") ||node.jjtGetChild(i).jjtGetChild(0).toString().equals("VariableType")) {
                   typeName = ((SimpleNode) node.jjtGetChild(i).jjtGetChild(0)).jjtGetName().toString();
                }
                else {
                    typeName = ((SimpleNode) node.jjtGetChild(i).jjtGetChild(0))
                        .toString();
                }
                String paramName;

                SimpleNode nextNode = (SimpleNode) node.jjtGetChild(i).jjtGetChild(1);
                if (nextNode.toString() == "Array") {
                    typeName += "Array";
                    paramName = ((SimpleNode) node.jjtGetChild(i).jjtGetChild(2)).jjtGetName().toString();
                } else {
                    paramName = nextNode.jjtGetName().toString();
                }

                SymbolVar param = new SymbolVar(typeName, paramName);
                String paramKey = paramName;

                if (!table.addParam(paramKey, param))
                    this.addError(new RuntimeException(
                            "Semantic error (line " + ((SimpleNode) node.jjtGetChild(i)).jjtGetLine()
                                    + "): The argument " + paramName + " is already declared").toString());

            } else if (node.jjtGetChild(i) instanceof ASTArgName) {
                String paramName = ((SimpleNode) node.jjtGetChild(i)).jjtGetName().toString();

                SymbolVar param = new SymbolVar("StringArray", paramName);
                String paramKey = paramName;

                if (!table.addParam(paramKey, param))
                    this.addError(new RuntimeException(
                            "Semantic error (line " + ((SimpleNode) node.jjtGetChild(i)).jjtGetLine()
                                    + "):The argument " + paramName + " is already declared").toString());

            } else if (node.jjtGetChild(i) instanceof ASTVarDeclaration) {
                String typeName ;
                if(node.jjtGetChild(i).jjtGetChild(0).toString().equals("Identifier") || node.jjtGetChild(i).jjtGetChild(0).toString().equals("VariableType")) {
                   typeName = ((SimpleNode) node.jjtGetChild(i).jjtGetChild(0)).jjtGetName().toString();
                }
                else {
                    typeName = ((SimpleNode)node.jjtGetChild(i).jjtGetChild(0))
                        .toString();
                }
        
                String varName;
                SimpleNode nextNode = (SimpleNode) node.jjtGetChild(i).jjtGetChild(1);

                if (nextNode.toString() == "Array") {
                    typeName += "Array";
                    varName = ((SimpleNode) node.jjtGetChild(i).jjtGetChild(2)).jjtGetName().toString();
                } else {
                    varName = nextNode.jjtGetName().toString();
                }

                SymbolVar s = new SymbolVar(typeName, varName);
                String key = varName;

                if (!table.add(key, s))
                    this.addError(new RuntimeException(
                            "Semantic error (line " + ((SimpleNode) node.jjtGetChild(i)).jjtGetLine()
                                    + "): The variable " + varName + " is already declared").toString());

            } else if (node.jjtGetChild(i) instanceof ASTImportDeclaration) {
                String name = "";
                Boolean staticImport = false;

                for (int j = 0; j < node.jjtGetChild(i).jjtGetNumChildren() - 1; j++) {

                    if (node.jjtGetChild(i).jjtGetChild(j).toString() == "STATIC") {
                        staticImport = true;
                        continue;
                    }

                    if (node.jjtGetChild(i).jjtGetChild(j).toString().equals("Identifier")) {
                        String className = ((SimpleNode) node.jjtGetChild(i).jjtGetChild(j)).jjtGetName().toString();
                        if (!staticImport) {
                            if (!table.hasKey("import_" + className))
                                this.addError(new RuntimeException(
                                        "Semantic error (line " + ((SimpleNode) node.jjtGetChild(i)).jjtGetLine()
                                                + "): Cannot import this method because the class " + className
                                                + " has not been imported").toString());
                        }
                        name += className;

                    } else if (node.jjtGetChild(i).jjtGetChild(j).toString().equals("FunctionCall")) {
                        name += ".";
                        name += ((SimpleNode) node.jjtGetChild(i).jjtGetChild(j).jjtGetChild(0)).jjtGetName()
                                .toString();
                        name += "(";

                        for (int k = 0; k < node.jjtGetChild(i).jjtGetChild(j).jjtGetChild(1)
                                .jjtGetNumChildren(); k++) {

                            String arg = node.jjtGetChild(i).jjtGetChild(j).jjtGetChild(1).jjtGetChild(k).toString();
                            if (arg.equals("Array")) {
                                name += arg;
                                continue;
                            }

                            if (k != 0)
                                name += ",";

                            if (arg.equals("VOID"))
                                break;
                            name += arg;
                        }
                        name += ")";
                    }
                }

                String returnType = "";
                if (node.jjtGetChild(i).jjtGetChild(node.jjtGetChild(i).jjtGetNumChildren() - 1).toString()
                        .equals("FunctionCall")) {
                    name += ".";

                    name += ((SimpleNode) node.jjtGetChild(i).jjtGetChild(node.jjtGetChild(i).jjtGetNumChildren() - 1)
                            .jjtGetChild(0)).jjtGetName().toString();
                    name += "(";

                    for (int k = 0; k < node.jjtGetChild(i).jjtGetChild(node.jjtGetChild(i).jjtGetNumChildren() - 1)
                            .jjtGetChild(1).jjtGetNumChildren(); k++) {
                        if (k != 0)
                            name += ",";

                        String arg = node.jjtGetChild(i).jjtGetChild(node.jjtGetChild(i).jjtGetNumChildren() - 1)
                                .jjtGetChild(1).jjtGetChild(k).toString();

                        if (arg.equals("VOID"))
                            break;
                        name += arg;
                    }
                    name += ")";
                    returnType = "Void";

                } else if (node.jjtGetChild(i).jjtGetChild(node.jjtGetChild(i).jjtGetNumChildren() - 1).toString()
                        .equals("Identifier")) {
                    name = ((SimpleNode) node.jjtGetChild(i).jjtGetChild(node.jjtGetChild(i).jjtGetNumChildren() - 1))
                            .jjtGetName().toString();
                    returnType = "Void";
                } else {
                    if (node.jjtGetChild(i).jjtGetChild(node.jjtGetChild(i).jjtGetNumChildren() - 1).toString()
                            .equals("Array"))
                        returnType = "IntArray";
                    else
                        returnType = node.jjtGetChild(i).jjtGetChild(node.jjtGetChild(i).jjtGetNumChildren() - 1)
                                .toString();
                }
                SymbolImport s = new SymbolImport(returnType, name, staticImport);
                String key = "import_" + name;

                if (!table.add(key, s))
                    if (!table.get(key).getType().equals(returnType))
                        this.addError(new RuntimeException(
                                "Semantic error (line " + ((SimpleNode) node.jjtGetChild(i)).jjtGetLine()
                                        + "): The import " + name + " is already declared with a different return type")
                                                .toString());
            }
        }

    }

    public SymbolTable getClassScope(SimpleNode node, SymbolTable scope) {
        String name = ((SimpleNode) node.jjtGetChild(0)).jjtGetName().toString();
        String key = "class_" + name;
        SymbolCM s = (SymbolCM) scope.get(key);
        return s.getSymbolTable();
    }

    public SymbolTable getMethodScope(SimpleNode node, SymbolTable scope) {
        String methodType = ((SimpleNode) node.jjtGetChild(1)).toString();
        String methodName;
        SimpleNode arguments;
        String key;

        if (!((SimpleNode) node.jjtGetChild(1)).toString().equals("Static_Void_Main")) {
            SimpleNode nextNode = (SimpleNode) node.jjtGetChild(2);
            int nodeIndex;

            if (nextNode.toString() == "Array") {
                methodType += "Array";
                methodName = ((SimpleNode) node.jjtGetChild(3)).jjtGetName().toString();
                nodeIndex = 4;
            } else {
                methodName = ((SimpleNode) node.jjtGetChild(2)).jjtGetName().toString();
                nodeIndex = 3;
            }

            int argCounter = 0;
            String argTypes = "";

            while (((SimpleNode) node.jjtGetChild(nodeIndex)).toString() == "Argument") {

                String typeName ;
                if(node.jjtGetChild(nodeIndex).jjtGetChild(0).toString().equals("Identifier") ||node.jjtGetChild(nodeIndex).jjtGetChild(0).toString().equals("VariableType")) {
                   typeName = ((SimpleNode) node.jjtGetChild(nodeIndex).jjtGetChild(0)).jjtGetName().toString();
                }
                else {
                    typeName = ((SimpleNode) node.jjtGetChild(nodeIndex).jjtGetChild(0))
                        .toString();
                }                

                argTypes += "_" + typeName;

                String array = ((SimpleNode) node.jjtGetChild(nodeIndex).jjtGetChild(1)).toString();
                if (array.equals("Array"))
                    argTypes += "Array";

                argCounter++;
                nodeIndex++;
            }

            key = methodName + "_" + argCounter + argTypes;

        } else {
            methodName = "main";
            key = "static_void_main_1";
        }
        SymbolCM s = (SymbolCM) scope.get(key);
        return s.getSymbolTable();
    }

    public boolean isVarDeclared(String varName, SymbolTable scope) {
        while (scope.getTable() != null) {
            if (scope.hasKey(varName))
                return true;

            scope = scope.getParent();
        }

        return false;
    }

    public String getIndexofArray(String varName, SimpleNode node) {
        String ret = "";

        do {
            SimpleNode child = node;
            node = (SimpleNode) node.jjtGetParent();

            int limit = Arrays.asList(node.jjtGetChildren()).indexOf(child);

            String init = getIndexOfArrayInScope(varName, node, limit);
            if (init.equals("true"))
                return init;
            else if (init.equals("might"))
                ret = init;

        } while (node.jjtGetParent() != null);

        return ret;
    }

    public String getIndexOfArrayInScope(String varName, SimpleNode node, int limit) {
        String ret = "false";
        int alreadyInIf = -1;

        for (int i = 0; i < limit; i++) {

            if (node.jjtGetChild(i) instanceof ASTAssigned) {
                if (node.jjtGetChild(i).jjtGetChild(0).toString().equals("Variable")) {
                    if (((SimpleNode) node.jjtGetChild(i).jjtGetChild(0)).jjtGetName().toString().equals(varName))
                        return "true";
                }
            } else if (node.jjtGetChild(i) instanceof ASTIfStatement) {

                if (!isVarInitializedInScope(varName, (SimpleNode) node.jjtGetChild(i).jjtGetChild(1),
                        node.jjtGetChild(i).jjtGetChild(1).jjtGetNumChildren()).equals("false")) {
                    ret = "might";
                    alreadyInIf = i;
                }
            } else if (node.jjtGetChild(i) instanceof ASTElseStatement
                    || node.jjtGetChild(i) instanceof ASTWhileStatement) {

                if (!isVarInitializedInScope(varName, (SimpleNode) node.jjtGetChild(i),
                        node.jjtGetChild(i).jjtGetNumChildren()).equals("false")) {

                    if (alreadyInIf + 1 == i)
                        return "true";

                    ret = "might";
                    alreadyInIf = -1;
                }
            }
        }

        return ret;
    }

    public String isVarInitialized(String varName, SimpleNode node) {
        String ret = "false";
        do {
            SimpleNode child = node;
            node = (SimpleNode) node.jjtGetParent();

            int limit = Arrays.asList(node.jjtGetChildren()).indexOf(child);

            String init = isVarInitializedInScope(varName, node, limit);
            if (init.equals("true"))
                return init;
            else if (init.equals("might"))
                ret = init;

        } while (node.jjtGetParent() != null);

        // Check if its a class attribute
        for (String key : this.globalTable.getTable().keySet()) {
            String[] words = key.split("_");

            if (words[0].equals("class")) {
                if (((SymbolCM) this.globalTable.get(key)).getSymbolTable().hasKey(varName))
                    return "true";
                break;
            }
        }

        return ret;
    }

    public String isVarInitializedInScope(String varName, SimpleNode node, int limit) {
        String ret = "false";
        int alreadyInIf = -1;

        for (int i = 0; i < limit; i++) {

            if (node.jjtGetChild(i) instanceof ASTArgument) {
                String paramName;

                SimpleNode nextNode = (SimpleNode) node.jjtGetChild(i).jjtGetChild(1);
                if (nextNode.toString() == "Array") {
                    paramName = ((SimpleNode) node.jjtGetChild(i).jjtGetChild(2)).jjtGetName().toString();
                } else {
                    paramName = nextNode.jjtGetName().toString();
                }

                if (paramName.equals(varName))
                    return "true";

            } else if (node.jjtGetChild(i) instanceof ASTArgName) {
                String paramName = ((SimpleNode) node.jjtGetChild(i)).jjtGetName().toString();

                if (paramName.equals(varName))
                    return "true";

            } else if (node.jjtGetChild(i) instanceof ASTAssigned) {
                if (node.jjtGetChild(i).jjtGetChild(0).toString().equals("Variable")) {
                    if (((SimpleNode) node.jjtGetChild(i).jjtGetChild(0)).jjtGetName().toString().equals(varName))
                        return "true";
                }
            } else if (node.jjtGetChild(i) instanceof ASTIfStatement) {

                if (!isVarInitializedInScope(varName, (SimpleNode) node.jjtGetChild(i).jjtGetChild(1),
                        node.jjtGetChild(i).jjtGetChild(1).jjtGetNumChildren()).equals("false")) {
                    ret = "might";
                    alreadyInIf = i;
                }
            } else if (node.jjtGetChild(i) instanceof ASTElseStatement
                    || node.jjtGetChild(i) instanceof ASTWhileStatement) {

                if (!isVarInitializedInScope(varName, (SimpleNode) node.jjtGetChild(i),
                        node.jjtGetChild(i).jjtGetNumChildren()).equals("false")) {

                    if (alreadyInIf + 1 == i)
                        return "true";

                    ret = "might";
                    alreadyInIf = -1;
                }
            }
        }

        return ret;
    }

    public String getTypeIdentifier(SimpleNode identifier, SymbolTable scope) {
        if (identifier instanceof ASTIdentifier) {
            String varName = identifier.jjtGetName().toString();

            while (scope.getTable() != null) {
                if (scope.hasKey(varName)) {
                    Symbol var = scope.get(varName);
                    return var.getType();
                }

                scope = scope.getParent();
            }
        } else if (identifier instanceof ASTValue) {
            if (identifier.jjtGetChild(0) instanceof ASTLength) {
                return "Int";
            }
            if (identifier.jjtGetNumChildren() == 1)
                return getTypeIdentifier((SimpleNode) identifier.jjtGetChild(0), scope);
            else
                return getFunctionCallType(identifier, scope);

        } else if (identifier instanceof ASTInteger) {
            return "Int";

        } else if (identifier instanceof ASTVariable) {
            String varName = identifier.jjtGetName().toString();

            while (scope.getTable() != null) {
                if (scope.hasKey(varName)) {
                    Symbol var = scope.get(varName);
                    return var.getType();
                }

                scope = scope.getParent();
            }
        } else if (identifier instanceof ASTFalse || identifier instanceof ASTTrue) {
            return "Boolean";

        } else if (identifier instanceof ASTArrayAccess) {
            String varName = ((SimpleNode) identifier.jjtGetChild(0)).jjtGetName().toString();
            String type = "";

            while (scope.getTable() != null) {
                if (scope.hasKey(varName)) {
                    Symbol var = scope.get(varName);
                    type = var.getType();
                    break;
                }

                scope = scope.getParent();
            }

            if (type.equals("IntArray"))
                return "Int";
            else if (type.equals("StringArray"))
                return "String";

            return null;

        } else if (identifier instanceof ASTAssignExp) {
            if (identifier.jjtGetNumChildren() == 1) {
                return getTypeIdentifier((SimpleNode) identifier.jjtGetChild(0), scope);
            }
            else {
                return getFunctionCallType(identifier, scope);
            }

        } else if (identifier instanceof ASTAnd || identifier instanceof ASTLessThan) {
            return "Boolean";

        } else if (identifier instanceof ASTAdd || identifier instanceof ASTSubtract
                || identifier instanceof ASTMultiply || identifier instanceof ASTDivide) {
            return "Int";

        } else if (identifier instanceof ASTNewInstance) {
            if (identifier.jjtGetNumChildren() == 1)
                return ((SimpleNode) identifier.jjtGetChild(0)).jjtGetName().toString();
            else if (identifier.jjtGetNumChildren() == 2)
                return ((SimpleNode) identifier.jjtGetChild(1)).toString();

        } else if (identifier instanceof ASTThis) {
            // check if method exists in current class
            String className = "";

            for (String key : this.globalTable.getTable().keySet()) {
                String[] words = key.split("_");

                if (words[0].equals("class")) {
                    className = this.globalTable.get(key).getName();
                    break;
                }
            }

            String methodName = ((SimpleNode) identifier.jjtGetChild(0).jjtGetChild(0)).jjtGetName().toString();
            int numArg = identifier.jjtGetChild(0).jjtGetChild(1).jjtGetNumChildren();

            // get method key
            String method_key = "";
            method_key += methodName;
            method_key += "_" + numArg;
            for (int j = 0; j < numArg; j++)
                method_key += "_" + getTypeIdentifier(
                        (SimpleNode) identifier.jjtGetChild(0).jjtGetChild(1).jjtGetChild(j), scope);

            if (((SymbolCM) this.globalTable.get("class_" + className)).getSymbolTable().hasKey(method_key))
                return ((SymbolCM) this.globalTable.get("class_" + className)).getSymbolTable().get(method_key)
                        .getType();
        } else if (identifier instanceof ASTNot) {
            return "Boolean";
        }

        return "";
    }

    public String getFunctionCallType(SimpleNode identifier, SymbolTable scope) {
        SimpleNode classVar = (SimpleNode) identifier.jjtGetChild(0);
        String className = getTypeIdentifier(classVar, scope);

        if (this.globalTable.hasKey("class_" + className)) {
            String methodName = ((SimpleNode) identifier.jjtGetChild(1).jjtGetChild(0)).jjtGetName().toString();
            int numArg = identifier.jjtGetChild(1).jjtGetChild(1).jjtGetNumChildren();

            // get method key
            String method_key = "";
            method_key += methodName;
            method_key += "_" + numArg;

            for (int j = 0; j < numArg; j++)
                method_key += "_" + getTypeIdentifier(
                        (SimpleNode) identifier.jjtGetChild(1).jjtGetChild(1).jjtGetChild(j), scope);

            SymbolTable classTable = ((SymbolCM) this.globalTable.get("class_" + className)).getSymbolTable();
            if (classTable.hasKey(method_key))
                return classTable.get(method_key).getType();

        } else {

            // Check Extends
            String extendClass = null;
            for (String key : this.globalTable.getTable().keySet()) {
                String[] words = key.split("_");

                if (words[0].equals("class")) {
                    if (((SymbolCM) this.globalTable.get(key)).getSymbolTable().hasKey("extends"))
                        extendClass = ((SymbolCM) this.globalTable.get(key)).getSymbolTable().getTable().get("extends")
                                .getName();
                }
            }

            if (className.equals(extendClass)) {

            } else {

                String methodName = ((SimpleNode) identifier.jjtGetChild(1).jjtGetChild(0)).jjtGetName().toString();
                int numArg = identifier.jjtGetChild(1).jjtGetChild(1).jjtGetNumChildren();

                // get import key
                String importKey = "import_" + classVar.jjtGetName().toString() + "." + methodName + "(";
                for (int j = 0; j < numArg; j++) {
                    if (j != 0)
                        importKey += ",";

                    importKey += getTypeIdentifier((SimpleNode) identifier.jjtGetChild(1).jjtGetChild(1).jjtGetChild(j),
                            scope);
                }
                importKey += ")";

                if (this.globalTable.hasKey(importKey))
                    return this.globalTable.get(importKey).getType();
            }
        }

        return "";
    }

    public boolean isBoolean(SimpleNode expression, SymbolTable scope) {
        if (expression instanceof ASTLessThan) {
            SimpleNode var1 = (SimpleNode) expression.jjtGetChild(0);
            SimpleNode var2 = (SimpleNode) expression.jjtGetChild(1);

            String type1 = getTypeIdentifier(var1, scope);
            String type2 = getTypeIdentifier(var2, scope);

            if (type1 != "" && type1 == type2) {
                return true;
            }
            return false;

        } else if (expression instanceof ASTAnd) {
            if (isBoolean((SimpleNode) expression.jjtGetChild(0), scope)
                    && isBoolean((SimpleNode) expression.jjtGetChild(1), scope)) {
                return true;
            }
            return false;

        } else if (expression instanceof ASTFalse || expression instanceof ASTTrue) {
            return true;

        } else if (expression instanceof ASTIdentifier) {
            String varName = ((SimpleNode) expression).jjtGetName().toString();

            while (scope.getTable() != null) {

                if (scope.hasKey(varName)) {
                    Symbol var = scope.get(varName);
                    if (var.getType() == "Boolean") {
                        return true;
                    }
                    return false;
                }

                scope = scope.getParent();
            }

        } else if (expression instanceof ASTValue) {
            String type = getTypeIdentifier(expression, scope);
            return type.equals("Boolean");

        } else if (expression instanceof ASTThis) {
            return getTypeIdentifier(expression, scope).equals("Boolean");
        } else if (expression instanceof ASTNot) {
            return isBoolean((SimpleNode) expression.jjtGetChild(0), scope);
        }

        return false;
    }

    public void semanticAnalysis(SimpleNode node) throws Exception {
        semanticAnalysis(node, this.globalTable);
    }

    public void semanticAnalysis(SimpleNode node, SymbolTable scope) throws Exception {
        SymbolTable newScope = null;
        for (int i = 0; i < node.jjtGetNumChildren(); i++) {
            if (node.jjtGetChild(i) instanceof ASTClass) {
                newScope = getClassScope((SimpleNode) node.jjtGetChild(i), scope);
                if (scope == null)
                    return;
                semanticAnalysis((SimpleNode) node.jjtGetChild(i), newScope);

            } else if (node.jjtGetChild(i) instanceof ASTMethod) {
                newScope = getMethodScope((SimpleNode) node.jjtGetChild(i), scope);
                if (scope == null)
                    return;

                semanticAnalysis((SimpleNode) node.jjtGetChild(i), newScope);

            } else if (node.jjtGetChild(i) instanceof ASTVariable) {
                String varName = ((SimpleNode) node.jjtGetChild(i)).jjtGetName().toString();
                if (!isVarDeclared(varName, scope)) {
                    // error var not defined
                    this.addError(new RuntimeException(
                            "Semantic error (line " + ((SimpleNode) node.jjtGetChild(i)).jjtGetLine()
                                    + "): The variable " + varName + " is not declared").toString());
                }

            } else if (node.jjtGetChild(i) instanceof ASTAssigned) {
                semanticAnalysis((SimpleNode) node.jjtGetChild(i), scope);

                SimpleNode left = (SimpleNode) node.jjtGetChild(i).jjtGetChild(0);
                SimpleNode right = (SimpleNode) node.jjtGetChild(i).jjtGetChild(1);

                String left_type = getTypeIdentifier(left, scope);
                String right_type = getTypeIdentifier(right, scope);

                if (!left_type.equals(right_type)) {
                    this.addError(new RuntimeException("Semantic error (line "
                            + ((SimpleNode) node.jjtGetChild(i)).jjtGetLine() + "): Assignment expected " + left_type
                            + ", got " + (right_type == "" ? "null" : right_type) + " instead").toString());
                }

            } else if (node.jjtGetChild(i) instanceof ASTAssignExp) {
                semanticAnalysis((SimpleNode) node.jjtGetChild(i), scope);

            } else if (node.jjtGetChild(i) instanceof ASTAdd) {
                SimpleNode var1 = (SimpleNode) node.jjtGetChild(i).jjtGetChild(0);
                SimpleNode var2 = (SimpleNode) node.jjtGetChild(i).jjtGetChild(1);

                String type1 = getTypeIdentifier(var1, scope);
                String type2 = getTypeIdentifier(var2, scope);

                if (type1 == "" || !type1.equals(type2)) {
                    this.addError(new RuntimeException(
                            "Semantic error (line " + ((SimpleNode) node.jjtGetChild(i)).jjtGetLine()
                                    + "): Arithmetic operators can only be used with variables of the same type")
                                            .toString());
                }

                if (type1.equals("IntArray")) {
                    this.addError(new RuntimeException(
                            "Semantic error (line " + ((SimpleNode) node.jjtGetChild(i)).jjtGetLine()
                                    + "): Arrays cannot be used directly with arithmetic operators").toString());
                }

                if (type1.equals("Boolean")) {
                    this.addError(new RuntimeException(
                            "Semantic error (line " + ((SimpleNode) node.jjtGetChild(i)).jjtGetLine()
                                    + "): Booleans cannot be used with arithmetic operators").toString());
                }

                if (!type1.equals("Int")) {
                    this.addError(new RuntimeException(
                            "Semantic error (line " + ((SimpleNode) node.jjtGetChild(i)).jjtGetLine()
                                    + "): Only integers can be used with arithmetic operators").toString());
                }
                semanticAnalysis((SimpleNode) node.jjtGetChild(i), scope);

            } else if (node.jjtGetChild(i) instanceof ASTMultiply) {
                SimpleNode var1 = (SimpleNode) node.jjtGetChild(i).jjtGetChild(0);
                SimpleNode var2 = (SimpleNode) node.jjtGetChild(i).jjtGetChild(1);

                String type1 = getTypeIdentifier(var1, scope);
                String type2 = getTypeIdentifier(var2, scope);

                if (type1 == "" || !type1.equals(type2)) {
                    this.addError(new RuntimeException(
                            "Semantic error (line " + ((SimpleNode) node.jjtGetChild(i)).jjtGetLine()
                                    + "): Arithmetic operators can only be used with variables of the same type")
                                            .toString());
                }

                if (type1.equals("IntArray")) {
                    this.addError(new RuntimeException(
                            "Semantic error (line " + ((SimpleNode) node.jjtGetChild(i)).jjtGetLine()
                                    + "): Arrays cannot be used directly with arithmetic operators").toString());
                }

                if (type1.equals("Boolean")) {
                    this.addError(new RuntimeException(
                            "Semantic error (line " + ((SimpleNode) node.jjtGetChild(i)).jjtGetLine()
                                    + "): Booleans cannot be used with arithmetic operators").toString());
                }

                if (!type1.equals("Int")) {
                    this.addError(new RuntimeException(
                            "Semantic error (line " + ((SimpleNode) node.jjtGetChild(i)).jjtGetLine()
                                    + "): Only integers can be used with arithmetic operators").toString());
                }

                semanticAnalysis((SimpleNode) node.jjtGetChild(i), scope);

            } else if (node.jjtGetChild(i) instanceof ASTDivide) {
                SimpleNode var1 = (SimpleNode) node.jjtGetChild(i).jjtGetChild(0);
                SimpleNode var2 = (SimpleNode) node.jjtGetChild(i).jjtGetChild(1);

                String type1 = getTypeIdentifier(var1, scope);
                String type2 = getTypeIdentifier(var2, scope);
                if (type1 == "" || !type1.equals(type2)) {
                    this.addError(new RuntimeException(
                            "Semantic error (line " + ((SimpleNode) node.jjtGetChild(i)).jjtGetLine()
                                    + "): Arithmetic operators can only be used with variables of the same type")
                                            .toString());
                }

                if (type1.equals("IntArray")) {
                    this.addError(new RuntimeException(
                            "Semantic error (line " + ((SimpleNode) node.jjtGetChild(i)).jjtGetLine()
                                    + "): Arrays cannot be used directly with arithmetic operators").toString());
                }

                if (type1.equals("Boolean")) {
                    this.addError(new RuntimeException(
                            "Semantic error (line " + ((SimpleNode) node.jjtGetChild(i)).jjtGetLine()
                                    + "): Booleans cannot be used with arithmetic operators").toString());
                }

                if (!type1.equals("Int")) {
                    this.addError(new RuntimeException(
                            "Semantic error (line " + ((SimpleNode) node.jjtGetChild(i)).jjtGetLine()
                                    + "): Only integers can be used with arithmetic operators").toString());
                }
                semanticAnalysis((SimpleNode) node.jjtGetChild(i), scope);

            } else if (node.jjtGetChild(i) instanceof ASTSubtract) {
                SimpleNode var1 = (SimpleNode) node.jjtGetChild(i).jjtGetChild(0);
                SimpleNode var2 = (SimpleNode) node.jjtGetChild(i).jjtGetChild(1);
                String type1 = getTypeIdentifier(var1, scope);
                String type2 = getTypeIdentifier(var2, scope);

                if (type1 == "" || !type1.equals(type2)) {
                    this.addError(new RuntimeException(
                            "Semantic error (line " + ((SimpleNode) node.jjtGetChild(i)).jjtGetLine()
                                    + "): Arithmetic operators can only be used with variables of the same type")
                                            .toString());
                }

                if (type1.equals("IntArray")) {
                    this.addError(new RuntimeException(
                            "Semantic error (line " + ((SimpleNode) node.jjtGetChild(i)).jjtGetLine()
                                    + "): Arrays cannot be used directly with arithmetic operators").toString());
                }

                if (type1.equals("Boolean")) {
                    this.addError(new RuntimeException(
                            "Semantic error (line " + ((SimpleNode) node.jjtGetChild(i)).jjtGetLine()
                                    + "): Booleans cannot be used with arithmetic operators").toString());
                }

                if (!type1.equals("Int")) {
                    this.addError(new RuntimeException(
                            "Semantic error (line " + ((SimpleNode) node.jjtGetChild(i)).jjtGetLine()
                                    + "): Only integers can be used with arithmetic operators").toString());
                }
                semanticAnalysis((SimpleNode) node.jjtGetChild(i), scope);

            } else if (node.jjtGetChild(i) instanceof ASTAnd) {
                SimpleNode var1 = (SimpleNode) node.jjtGetChild(i).jjtGetChild(0);
                SimpleNode var2 = (SimpleNode) node.jjtGetChild(i).jjtGetChild(1);

                if (!isBoolean((SimpleNode) node.jjtGetChild(i).jjtGetChild(0), scope)
                        || !isBoolean((SimpleNode) node.jjtGetChild(i).jjtGetChild(1), scope)) {
                    this.addError(new RuntimeException(
                            "Semantic error (line " + ((SimpleNode) node.jjtGetChild(i)).jjtGetLine()
                                    + "): Boolean operators can only be used with boolean expressions").toString());
                }
                semanticAnalysis((SimpleNode) node.jjtGetChild(i), scope);

            } else if (node.jjtGetChild(i) instanceof ASTValue) {
                SimpleNode firstChild = ((SimpleNode) node.jjtGetChild(i).jjtGetChild(0));
                String varName = "";

                if (firstChild.toString().equals("Identifier") && node.jjtGetChild(i).jjtGetNumChildren() == 1) {
                    varName = firstChild.jjtGetName().toString();
                }

                if (varName != "") {
                    if (!isVarDeclared(varName, scope))
                        this.addError(new RuntimeException(
                                "Semantic error (line " + ((SimpleNode) node.jjtGetChild(i)).jjtGetLine()
                                        + "): The variable " + varName + " is not declared").toString());
                    switch (isVarInitialized(varName, (SimpleNode) node.jjtGetChild(i))) {
                        case "true": {
                            break;
                        }
                        case "false": {
                            this.addError(new RuntimeException(
                                    "Semantic error (line " + ((SimpleNode) node.jjtGetChild(i)).jjtGetLine()
                                            + "): The variable " + varName + " is not initialized").toString());
                            break;
                        }
                        case "might": {
                            this.addError(new RuntimeException(
                                    "Semantic error (line " + ((SimpleNode) node.jjtGetChild(i)).jjtGetLine()
                                            + "): The variable " + varName + " might not be initialized").toString());
                            break;
                        }
                    }
                }

                semanticAnalysis((SimpleNode) node.jjtGetChild(i), scope);

            } else if (node.jjtGetChild(i) instanceof ASTLength) {
                String varType = getTypeIdentifier((SimpleNode) node.jjtGetChild(i).jjtGetChild(0), scope);

                if (!varType.equals("IntArray") && !varType.equals("StringArray"))
                    this.addError(new RuntimeException(
                            "Semantic error (line " + ((SimpleNode) node.jjtGetChild(i).jjtGetChild(0)).jjtGetLine()
                                    + "): The variable must be an array, instead of "
                                    + (varType == "" ? "null" : varType) + ", to have the length field").toString());

            } else if (node.jjtGetChild(i) instanceof ASTNewInstance) {
                if (node.jjtGetChild(i).jjtGetNumChildren() == 2) {
                    String type = getTypeIdentifier((SimpleNode) node.jjtGetChild(i).jjtGetChild(0), scope);
                    if (type != "Integer" && type != "Int")
                        this.addError(new RuntimeException(
                                "Semantic error (line " + ((SimpleNode) node.jjtGetChild(i)).jjtGetLine()
                                        + "): The size of an array must be an integer").toString());
                }
                semanticAnalysis((SimpleNode) node.jjtGetChild(i), scope);

            } else if (node.jjtGetChild(i) instanceof ASTArrayAccess) {
                SimpleNode var = (SimpleNode) node.jjtGetChild(i).jjtGetChild(0);
                String indexAccessed = ((SimpleNode) node.jjtGetChild(i).jjtGetChild(1).jjtGetChild(0)).jjtGetName()
                        .toString();
                int maxIndex;
                String type = getTypeIdentifier(var, scope);

                if (!type.equals("IntArray") && !type.equals("StringArray")) {
                    this.addError(new RuntimeException(
                            "Semantic error (line " + ((SimpleNode) node.jjtGetChild(i)).jjtGetLine()
                                    + "): An array access can only be made to an array variable").toString());
                }

                String varName = var.jjtGetName().toString();
                if (!isVarDeclared(varName, scope))
                    this.addError(new RuntimeException(
                            "Semantic error (line " + ((SimpleNode) node.jjtGetChild(i)).jjtGetLine()
                                    + "): The variable " + varName + " is not declared").toString());
                switch (isVarInitialized(varName, (SimpleNode) node.jjtGetChild(i))) {
                    case "true": {
                        // if(!verifyArrayBounds(varName, (SimpleNode) node.jjtGetChild(i)))
                        // this.addError(new RuntimeException(
                        // "Semantic error (line " + ((SimpleNode) node.jjtGetChild(i)).jjtGetLine()
                        // + "): Array out of bounds");
                        break;
                    }
                    case "false": {
                        this.addError(new RuntimeException(
                                "Semantic error (line " + ((SimpleNode) node.jjtGetChild(i)).jjtGetLine()
                                        + "): The variable " + varName + " is not initialized").toString());
                        break;
                    }
                    case "might": {
                        this.addError(new RuntimeException(
                                "Semantic error (line " + ((SimpleNode) node.jjtGetChild(i)).jjtGetLine()
                                        + "): The variable " + varName + " might not be initialized").toString());
                        break;
                    }

                }

                semanticAnalysis((SimpleNode) node.jjtGetChild(i), scope);

            } else if (node.jjtGetChild(i) instanceof ASTIndex) {
                String type = getTypeIdentifier((SimpleNode) node.jjtGetChild(i).jjtGetChild(0), scope);

                if (type != "Integer" && type != "Int") {
                    this.addError(new RuntimeException(
                            "Semantic error (line " + ((SimpleNode) node.jjtGetChild(i)).jjtGetLine()
                                    + "): The index of an array must be an integer").toString());
                }

            } else if (node.jjtGetChild(i) instanceof ASTIfStatement) {
                semanticAnalysis((SimpleNode) node.jjtGetChild(i), scope);

            } else if (node.jjtGetChild(i) instanceof ASTWhileStatement) {
                semanticAnalysis((SimpleNode) node.jjtGetChild(i), scope);

            } else if (node.jjtGetChild(i) instanceof ASTCondition) {
                if (!isBoolean((SimpleNode) node.jjtGetChild(i).jjtGetChild(0), scope)) {
                    this.addError(new RuntimeException(
                            "Semantic error (line " + ((SimpleNode) node.jjtGetChild(i)).jjtGetLine()
                                    + "): A condition must be an expression of a boolean type").toString());
                }
                semanticAnalysis((SimpleNode) node.jjtGetChild(i), scope);

            } else if (node.jjtGetChild(i) instanceof ASTIfBody) {
                semanticAnalysis((SimpleNode) node.jjtGetChild(i), scope);

            } else if (node.jjtGetChild(i) instanceof ASTElseStatement) {
                semanticAnalysis((SimpleNode) node.jjtGetChild(i), scope);

            } else if (node.jjtGetChild(i) instanceof ASTReturn) {
                String type = getTypeIdentifier((SimpleNode) node.jjtGetChild(i).jjtGetChild(0), scope);

                if (!type.equals(scope.getReturn().getType())) {
                    this.addError(new RuntimeException(
                            "Semantic error (line " + ((SimpleNode) node.jjtGetChild(i)).jjtGetLine()
                                    + "): The returned variable must be of same type as the method type.").toString());
                }
                semanticAnalysis((SimpleNode) node.jjtGetChild(i), scope);

            } else if (node.jjtGetChild(i) instanceof ASTFunctionCall) {
                SimpleNode classVar = (SimpleNode) node.jjtGetChild(i - 1);

                String className = getTypeIdentifier(classVar, scope);

                // Check Class
                if (this.globalTable.hasKey("class_" + className)) {
                    if (!(classVar instanceof ASTNewInstance)) {
                        String varName = classVar.jjtGetName().toString();

                        if (!isVarDeclared(varName, scope)) {
                            // error var not defined
                            this.addError(new RuntimeException(
                                    "Semantic error (line " + ((SimpleNode) node.jjtGetChild(i)).jjtGetLine()
                                            + "): The variable " + varName + " is not declared").toString());
                        }

                        switch (isVarInitialized(varName, (SimpleNode) node.jjtGetChild(i))) {
                            case "true": {
                                break;
                            }
                            case "false": {
                                this.addError(new RuntimeException(
                                        "Semantic error (line " + ((SimpleNode) node.jjtGetChild(i)).jjtGetLine()
                                                + "): The variable " + varName + " is not initialized").toString());
                                break;
                            }
                            case "might": {
                                this.addError(new RuntimeException(
                                        "Semantic error (line " + ((SimpleNode) node.jjtGetChild(i)).jjtGetLine()
                                                + "): The variable " + varName + " might not be initialized")
                                                        .toString());
                                break;
                            }
                        }
                    }

                    String methodName = ((SimpleNode) node.jjtGetChild(i).jjtGetChild(0)).jjtGetName().toString();
                    int numArg = node.jjtGetChild(i).jjtGetChild(1).jjtGetNumChildren();

                    // get method key
                    String method_key = "";
                    method_key += methodName;
                    method_key += "_" + numArg;

                    for (int j = 0; j < numArg; j++)
                        method_key += "_" + getTypeIdentifier(
                                (SimpleNode) node.jjtGetChild(i).jjtGetChild(1).jjtGetChild(j), scope);

                    if (!((SymbolCM) this.globalTable.get("class_" + className)).getSymbolTable().hasKey(method_key)) {

                        ArrayList<String[]> methods = new ArrayList<String[]>();

                        for (String key : ((SymbolCM) this.globalTable.get("class_" + className)).getSymbolTable()
                                .getTable().keySet()) {
                            String[] words = key.split("_");

                            if (methodName.equals(words[0])) {
                                if (String.valueOf(numArg).equals(words[1])) {
                                    this.addError(new RuntimeException(
                                            "Semantic error (line " + ((SimpleNode) node.jjtGetChild(i)).jjtGetLine()
                                                    + "): The method " + methodName + " argument types do not match")
                                                            .toString());
                                }
                                methods.add(words);
                            }
                        }

                        if (!methods.isEmpty())
                            this.addError(new RuntimeException("Semantic error (line "
                                    + ((SimpleNode) node.jjtGetChild(i)).jjtGetLine() + "): The method " + methodName
                                    + " requires " + methods.get(0)[1] + " arguments").toString());

                        this.addError(new RuntimeException(
                                "Semantic error (line " + ((SimpleNode) node.jjtGetChild(i)).jjtGetLine()
                                        + "): The method " + methodName + " does not exist within the target class")
                                                .toString());
                    }

                } else {
                    // Check Imports
                    String methodName = ((SimpleNode) node.jjtGetChild(i).jjtGetChild(0)).jjtGetName().toString();
                    int numArg = node.jjtGetChild(i).jjtGetChild(1).jjtGetNumChildren();

                    // get import key
                    String importKey = "";
                    for (int j = 0; j < numArg; j++) {
                        if (j != 0)
                            importKey += ",";

                        importKey += getTypeIdentifier((SimpleNode) node.jjtGetChild(i).jjtGetChild(1).jjtGetChild(j),
                                scope);
                    }
                    importKey += ")";

                    String importStatic = "import_" + classVar.jjtGetName().toString() + "." + methodName + "("
                            + importKey;
                    String importClass = "import_" + className + "." + methodName + "(" + importKey;

                    if (!this.globalTable.hasKey(importStatic) && !this.globalTable.hasKey(importClass))
                        this.addError(new RuntimeException("Semantic error (line "
                                + ((SimpleNode) node.jjtGetChild(i)).jjtGetLine() + "): The method " + methodName
                                + " does not exist within the target class and is not imported").toString());
                }

                semanticAnalysis((SimpleNode) node.jjtGetChild(i), scope);

            } else if (node.jjtGetChild(i) instanceof ASTThis) {
                // check if method exists in current class
                String className = "";

                for (String key : this.globalTable.getTable().keySet()) {
                    String[] words = key.split("_");

                    if (words[0].equals("class"))
                        className = this.globalTable.get(key).getName();
                }

                String methodName = ((SimpleNode) node.jjtGetChild(i).jjtGetChild(0).jjtGetChild(0)).jjtGetName()
                        .toString();
                int numArg = node.jjtGetChild(i).jjtGetChild(0).jjtGetChild(1).jjtGetNumChildren();

                // get method key
                String method_key = "";
                method_key += methodName;
                method_key += "_" + numArg;
                for (int j = 0; j < numArg; j++)
                    method_key += "_" + getTypeIdentifier(
                            (SimpleNode) node.jjtGetChild(i).jjtGetChild(0).jjtGetChild(1).jjtGetChild(j), scope);

                if (!((SymbolCM) this.globalTable.get("class_" + className)).getSymbolTable().hasKey(method_key))
                    this.addError(new RuntimeException(
                            "Semantic error (line " + ((SimpleNode) node.jjtGetChild(i)).jjtGetLine() + "): The method "
                                    + methodName + " does not exist in this class").toString());

            } else if (node.jjtGetChild(i) instanceof ASTVarDeclaration) {
                if (node.jjtGetChild(i).jjtGetChild(0).toString().equals("VariableType")) {

                    String className = ((SimpleNode) node.jjtGetChild(i).jjtGetChild(0)).jjtGetName().toString();

                    if (!this.globalTable.hasKey("class_" + className)) {

                        String extendClass = "";
                        // Check Extends
                        for (String key : this.globalTable.getTable().keySet()) {
                            String[] words = key.split("_");

                            if (words[0].equals("class")) {
                                if (((SymbolCM) this.globalTable.get(key)).getSymbolTable().hasKey("extends"))
                                    extendClass = ((SymbolCM) this.globalTable.get(key)).getSymbolTable().getTable()
                                            .get("extends").getName();
                            }
                        }

                        if (!className.equals(extendClass)) {
                            if (!this.globalTable.hasKey("import_" + className))
                                this.addError(new RuntimeException("Semantic error (line "
                                        + ((SimpleNode) node.jjtGetChild(i)).jjtGetLine() + "): The variable type "
                                        + className + " cannot be found in this scope.").toString());
                        }
                    }
                }
                semanticAnalysis((SimpleNode) node.jjtGetChild(i), scope);

            } else if (node.jjtGetChild(i) instanceof ASTFunctionArguments) {
                semanticAnalysis((SimpleNode) node.jjtGetChild(i), scope);

            } else if (node.jjtGetChild(i) instanceof ASTNot) {
                if (!isBoolean((SimpleNode) node.jjtGetChild(i).jjtGetChild(0), scope)) {

                    this.addError(new RuntimeException(
                            "Semantic error (line " + ((SimpleNode) node.jjtGetChild(i)).jjtGetLine()
                                    + "): The boolean operator NOT (!) cannot be used with a non boolean expression")
                                            .toString());
                }
                semanticAnalysis((SimpleNode) node.jjtGetChild(i), scope);
            }
        }
    }

    public void printSymbolTables() {
        System.out.println("\nProgram");
        printImportSymbols(this.globalTable);
        printSymbolTables(this.globalTable, true);
    }

    public void printImportSymbols(SymbolTable global) {
        HashMap<String, Symbol> table = global.getTable();
        for (String key : table.keySet()) {
            Symbol s = table.get(key);
            if (s instanceof SymbolImport) {
                System.out.print("    import ");
                if (((SymbolImport) s).isStatic())
                    System.out.print("static ");
                System.out.println(s.getType() + " " + s.getName());
            }
        }
    }

    public void printSymbolTables(SymbolTable current, Boolean isClass) {
        HashMap<String, Symbol> table = current.getTable();
        for (String key : table.keySet()) {
            Symbol s = table.get(key);
            if (s instanceof SymbolCM) {
                if (s.getType() == "class") {
                    System.out.println("    " + s.getType() + " " + s.getName());
                } else {
                    isClass = false;
                    SymbolTable newTable = ((SymbolCM) s).getSymbolTable();
                    ArrayList<AbstractMap.SimpleEntry<String, Symbol>> params = newTable.getParams();
                    System.out.print("        method " + s.getType() + " " + s.getName() + "(");
                    for (int i = 0; i < params.size(); i++) {
                        AbstractMap.SimpleEntry<String, Symbol> param = params.get(i);
                        Symbol p = param.getValue();
                        if (i != 0)
                            System.out.print(", ");
                        System.out.print(p.getType() + " " + p.getName());
                    }
                    System.out.println(")");
                }
                printSymbolTables(((SymbolCM) s).getSymbolTable(), isClass);
            } else if (s instanceof SymbolVar) {
                if (isClass)
                    System.out.println("        var " + s.getType() + " " + s.getName());
                else
                    System.out.println("            var " + s.getType() + " " + s.getName());
            }
        }

    }

}