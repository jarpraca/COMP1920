import java.io.FileWriter;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.*;
import java.util.Stack;

public class CodeWriter {

    private String ENTER = "\n";
    private String TAB = "\t";
    private FileWriter fileWriter;
    private ArrayList<String> localVariables;
    private String className;
    private String extendsName;
    private String writeDefault;
    private boolean isStatic;
    private SemanticAnalysis analyzer;
    private int conditionCounter;
    private Stack<Integer> conditionDepth;
    // name, type
    private HashMap<String, String> attributes;

    private int maxStack = 0;
    private int totalStack = 0;

    public CodeWriter(String filename, SemanticAnalysis semanticAnalysis) {
        writeDefault = "global";
        localVariables = new ArrayList<String>();
        attributes = new HashMap<String, String>();
        analyzer = semanticAnalysis;
        try {
            File file = new File("jasminCode/" + filename + ".j");
            file.getParentFile().mkdirs();

            this.fileWriter = new FileWriter(file);
            conditionCounter = 0;
            conditionDepth = new Stack<Integer>();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void generateCode(SimpleNode node, SymbolTable global, String scope) {
        for (int i = 0; i < node.jjtGetNumChildren(); i++) {
            if (node.jjtGetChild(i) instanceof ASTClass) {
                String name = ((SimpleNode) node.jjtGetChild(i).jjtGetChild(0)).jjtGetName().toString();
                SymbolTable newScope = analyzer.getClassScope((SimpleNode) node.jjtGetChild(i), global);
                if (node.jjtGetChild(i).jjtGetChild(1) instanceof ASTExtends) {
                    this.extendsName = ((SimpleNode) node.jjtGetChild(i).jjtGetChild(2)).jjtGetName().toString();
                }
                else {
                    this.extendsName = "Object";
                }
                addClass(name, this.extendsName);

                if((node.jjtGetChild(i).jjtGetChild(1) instanceof ASTMethod) && !(writeDefault.equals("method"))) {
                    addDefaultConstructor();
                }

                generateCode((SimpleNode) node.jjtGetChild(i), newScope, "class");
                closeFile();

            } else if (node.jjtGetChild(i) instanceof ASTMethod) {
                writeDefault = "method";
                String methodName = "";
                String varName = "";
                for (int j = 0; j < node.jjtGetChild(i).jjtGetNumChildren(); j++) {
                    if (node.jjtGetChild(i).jjtGetChild(j) instanceof ASTMethodName)
                        methodName = ((SimpleNode) node.jjtGetChild(i).jjtGetChild(j)).jjtGetName().toString();
                    if (node.jjtGetChild(i).jjtGetChild(j) instanceof ASTStatic_Void_Main) {
                        methodName = "main";
                        varName = ((SimpleNode) node.jjtGetChild(i).jjtGetChild(j + 1)).jjtGetName().toString();
                    }
                }

                SymbolTable newScope = analyzer.getMethodScope((SimpleNode) node.jjtGetChild(i), global);

                if (methodName.equals("main")) {
                    addMain((SimpleNode) node.jjtGetChild(i), newScope);
                } else {
                    addMethod((SimpleNode) node.jjtGetChild(i), newScope, methodName);
                }

                generateCode((SimpleNode) node.jjtGetChild(i), newScope, "method");
                endMethod(newScope);

            } else if (node.jjtGetChild(i) instanceof ASTAssigned) {

                SimpleNode left = (SimpleNode) node.jjtGetChild(i).jjtGetChild(0);
                String left_type = analyzer.getTypeIdentifier(left, global);

                String left_name = "";
                if (left.jjtGetNumChildren() > 0) {
                    left_name = ((SimpleNode) left.jjtGetChild(0)).jjtGetName().toString();
                } else
                    left_name = left.jjtGetName().toString();

                if (localVariables.contains(left_name)
                        && node.jjtGetChild(i).jjtGetChild(0) instanceof ASTArrayAccess) {
                    int index;
                    if (this.isStatic)
                        index = localVariables.indexOf(left_name);
                    else
                        index = localVariables.indexOf(left_name) + 1;
                    if(index > 3)
                        writeCode(TAB + "aload " + index + ENTER);
                    else
                        writeCode(TAB + "aload_" + index + ENTER);
                } else if (attributes.containsKey(left_name) && !this.isStatic) {
                    writeCode(TAB + "aload_0" + ENTER);
                    if (node.jjtGetChild(i).jjtGetChild(0) instanceof ASTArrayAccess)
                        writeCode(TAB + "getfield " + this.className + "/" + left_name + " "
                                + getDescriptor(attributes.get(left_name)) + ENTER);
                }

                generateCode((SimpleNode) node.jjtGetChild(i), global, scope);

                if (node.jjtGetChild(i).jjtGetChild(0) instanceof ASTArrayAccess) {
                    writeCode(TAB + "iastore" + ENTER);
                } else {
                    addAssign(left_type, left_name);
                }

            } else if (node.jjtGetChild(i) instanceof ASTInteger) {
                addInteger(((SimpleNode) node.jjtGetChild(i)).jjtGetVal());
            } else if (node.jjtGetChild(i) instanceof ASTIndex) {
                generateCode((SimpleNode) node.jjtGetChild(i), global, scope);
            } else if (node.jjtGetChild(i) instanceof ASTTrue) {
                writeCode(TAB + "iconst_" + 1 + ENTER);
            } else if (node.jjtGetChild(i) instanceof ASTFalse) {
                writeCode(TAB + "iconst_" + 0 + ENTER);
            } else if (node.jjtGetChild(i) instanceof ASTAdd || node.jjtGetChild(i) instanceof ASTMultiply
                    || node.jjtGetChild(i) instanceof ASTSubtract || node.jjtGetChild(i) instanceof ASTDivide) {

                generateCode((SimpleNode) node.jjtGetChild(i), global, scope);
                addArithmeticExp((SimpleNode) node.jjtGetChild(i));
            } else if (node.jjtGetChild(i) instanceof ASTNewInstance) {

                if (node.jjtGetChild(i).jjtGetNumChildren() > 1
                        && node.jjtGetChild(i).jjtGetChild(1).toString().equals("IntArray")) {
                    if (((SimpleNode) node.jjtGetChild(i).jjtGetChild(0)).toString().equals("Integer")) {
                        addInteger(((SimpleNode) node.jjtGetChild(i).jjtGetChild(0)).jjtGetVal());

                    } else {
                        generateCode((SimpleNode) node.jjtGetChild(i), global, scope);
                    }
                    writeCode(TAB + "newarray int" + ENTER);

                } else {
                    invokeNewInstance(((SimpleNode) node.jjtGetChild(i).jjtGetChild(0)).jjtGetName().toString());
                }
            } else if (node.jjtGetChild(i) instanceof ASTFunctionArguments) {
                generateCode((SimpleNode) node.jjtGetChild(i), global, scope);

            } else if (node.jjtGetChild(i) instanceof ASTFunctionCall) {
                SimpleNode classVar = (SimpleNode) node.jjtGetChild(i - 1);
                String className = analyzer.getTypeIdentifier(classVar, global);

                String methodName = ((SimpleNode) node.jjtGetChild(i).jjtGetChild(0)).jjtGetName().toString();
                int numArg = node.jjtGetChild(i).jjtGetChild(1).jjtGetNumChildren();

                // get method key
                String method_key = "";
                method_key += methodName;
                method_key += "_" + numArg;

                for (int j = 0; j < numArg; j++)
                    method_key += "_" + analyzer
                            .getTypeIdentifier((SimpleNode) node.jjtGetChild(i).jjtGetChild(1).jjtGetChild(j), global);

            

                numArg = node.jjtGetChild(i).jjtGetChild(1).jjtGetNumChildren();
                String[] paramTypes = new String[numArg];

                for (int j = 0; j < numArg; j++) {
                    paramTypes[j] = analyzer
                            .getTypeIdentifier((SimpleNode) node.jjtGetChild(i).jjtGetChild(1).jjtGetChild(j), global);
                }

                generateCode((SimpleNode) node.jjtGetChild(i), global, scope);

                if (analyzer.getGlobalTable().hasKey("class_" + className)) {    
                    SymbolTable scope2 = global;
                String returnType = "";
                while (scope2.getTable() != null) {
                    if (scope2.hasKey(method_key)) {
                        Symbol var = scope2.get(method_key);
                        returnType = var.getType();
                        break;
                    }

                    scope2 = scope2.getParent();
                }
                    functionInvocation(className, (SimpleNode) node.jjtGetChild(i), paramTypes, returnType,
                            scope, global);

                    if ((node instanceof ASTMethod || node instanceof ASTIfBody || node instanceof ASTElseStatement
                    || node instanceof ASTWhileStatement) && !returnType.equals("Void") && !returnType.equals("")) {
                        writeCode(TAB + "pop" + ENTER);
                    }
                } else {
                    // Check Imports
                    methodName = ((SimpleNode) node.jjtGetChild(i).jjtGetChild(0)).jjtGetName().toString();

                    // get import key
                    String importKey = "";
                    for (int j = 0; j < numArg; j++) {
                        if (j != 0)
                            importKey += ",";

                        importKey += analyzer.getTypeIdentifier(
                                (SimpleNode) node.jjtGetChild(i).jjtGetChild(1).jjtGetChild(j), global);
                    }
                    importKey += ")";

                    String importStatic = "import_" + classVar.jjtGetName().toString() + "." + methodName + "("
                            + importKey;
                    String importClass = "import_" + className + "." + methodName + "(" + importKey;

                    SymbolImport importSymbol;
                    if (analyzer.getGlobalTable().hasKey(importStatic))
                        importSymbol = ((SymbolImport) analyzer.getGlobalTable().get(importStatic));
                    else
                        importSymbol = ((SymbolImport) analyzer.getGlobalTable().get(importClass));

                    if (importSymbol.isStatic()) {
                        staticInvocation(classVar.jjtGetName().toString(), (SimpleNode) node.jjtGetChild(i), paramTypes,
                                importSymbol.getType(), scope, global);
                    } else {
                        String parent = analyzer.getTypeIdentifier(classVar, global);
                        functionInvocation(parent, (SimpleNode) node.jjtGetChild(i), paramTypes, importSymbol.getType(),
                                scope, global);
                    }

                    if ((node instanceof ASTMethod || node instanceof ASTIfBody || node instanceof ASTElseStatement
                    || node instanceof ASTWhileStatement) && !importSymbol.getType().equals("Void") && !importSymbol.getType().equals("")) {
                writeCode(TAB + "pop" + ENTER);
            }
                }
              

            } else if (node.jjtGetChild(i) instanceof ASTThis) {

                writeCode(TAB + "aload_0" + ENTER);

                SimpleNode classVar = (SimpleNode) node;
                String className = this.className;

                String methodName = ((SimpleNode) node.jjtGetChild(i).jjtGetChild(0).jjtGetChild(0)).jjtGetName()
                        .toString();
                int numArg = node.jjtGetChild(i).jjtGetChild(0).jjtGetChild(1).jjtGetNumChildren();

                // get method key
                String method_key = "";
                method_key += methodName;
                method_key += "_" + numArg;

                for (int j = 0; j < numArg; j++)
                    method_key += "_" + analyzer.getTypeIdentifier(
                            (SimpleNode) node.jjtGetChild(i).jjtGetChild(0).jjtGetChild(1).jjtGetChild(j), global);

                SymbolTable scope2 = global;
                String returnType = "";
                while (scope2.getTable() != null) {
                    if (scope2.hasKey(method_key)) {
                        Symbol var = scope2.get(method_key);
                        returnType = var.getType();
                        break;
                    }

                    scope2 = scope2.getParent();
                }

                numArg = node.jjtGetChild(i).jjtGetChild(0).jjtGetChild(1).jjtGetNumChildren();
                String[] paramTypes = new String[numArg];
                String[] params = new String[numArg];

                for (int j = 0; j < numArg; j++) {
                    paramTypes[j] = analyzer.getTypeIdentifier(
                            (SimpleNode) node.jjtGetChild(i).jjtGetChild(0).jjtGetChild(1).jjtGetChild(j), global);
                }

                generateCode((SimpleNode) node.jjtGetChild(i).jjtGetChild(0), global, scope);

                functionInvocation(className, (SimpleNode) node.jjtGetChild(i).jjtGetChild(0), paramTypes,
                        returnType, scope, global);

                if ((node instanceof ASTMethod || node instanceof ASTIfBody || node instanceof ASTElseStatement
                        || node instanceof ASTWhileStatement) && !returnType.equals("Void") && !returnType.equals("")) {
                    writeCode(TAB + "pop" + ENTER);

                }

            } else if (node.jjtGetChild(i) instanceof ASTVariable) {
                generateCode((SimpleNode) node.jjtGetChild(i), global, scope);
            } else if (node.jjtGetChild(i) instanceof ASTAssignExp) {
                generateCode((SimpleNode) node.jjtGetChild(i), global, scope);
            } else if (node.jjtGetChild(i) instanceof ASTAnd) {
                generateCode((SimpleNode) node.jjtGetChild(i), global, scope);
                addAnd();
            } else if (node.jjtGetChild(i) instanceof ASTIdentifier) {
                if (localVariables.contains(((SimpleNode) node.jjtGetChild(i)).jjtGetName().toString())) {
                    int index;

                    if (this.isStatic)
                        index = localVariables.indexOf(((SimpleNode) node.jjtGetChild(i)).jjtGetName().toString());
                    else
                        index = localVariables.indexOf(((SimpleNode) node.jjtGetChild(i)).jjtGetName().toString()) + 1;
                    String typeCall = analyzer.getTypeIdentifier((SimpleNode) node.jjtGetChild(i), global);
                    String type = getDescriptor(typeCall);
                    if (type.equals("I") || type.equals("Z")) {
                        if (index > 3)
                            writeCode(TAB + "iload " + index + ENTER);
                        else
                            writeCode(TAB + "iload_" + index + ENTER);
                    } else {
                        if (index > 3)
                            writeCode(TAB + "aload " + index + ENTER);
                        else
                            writeCode(TAB + "aload_" + index + ENTER);

                    }
                } else if (attributes.containsKey(((SimpleNode) node.jjtGetChild(i)).jjtGetName().toString())) {
                    writeCode(TAB + "aload_0" + ENTER);
                    writeCode(TAB + "getfield " + this.className + "/"
                            + ((SimpleNode) node.jjtGetChild(i)).jjtGetName().toString() + " "
                            + getDescriptor(attributes.get(((SimpleNode) node.jjtGetChild(i)).jjtGetName().toString()))
                            + ENTER);
                }
            } else if (node.jjtGetChild(i) instanceof ASTValue) {
                generateCode((SimpleNode) node.jjtGetChild(i), global, scope);

                if (node.jjtGetChild(i).jjtGetChild(0) instanceof ASTArrayAccess) {
                    writeCode(TAB + "iaload" + ENTER);
                }

                // pop
                if ((node instanceof ASTMethod || node instanceof ASTIfBody || node instanceof ASTElseStatement
                        || node instanceof ASTWhileStatement) && node.jjtGetChild(i).jjtGetNumChildren() > 1
                        && node.jjtGetChild(i).jjtGetChild(1).toString().equals("FunctionCall")) {

                    SimpleNode classVar = (SimpleNode) node.jjtGetChild(i).jjtGetChild(0);

                    String methodName = ((SimpleNode) node.jjtGetChild(i).jjtGetChild(1).jjtGetChild(0)).jjtGetName()
                            .toString();
                    int numArg = node.jjtGetChild(i).jjtGetChild(1).jjtGetChild(1).jjtGetNumChildren();

                    // get method key
                    String method_key = "";
                    method_key += methodName;
                    method_key += "_" + numArg;

                    for (int j = 0; j < numArg; j++)
                        method_key += "_" + analyzer.getTypeIdentifier(
                                (SimpleNode) node.jjtGetChild(i).jjtGetChild(1).jjtGetChild(1).jjtGetChild(j), global);

                    SymbolTable scope2 = global;
                    String returnType = "";
                    while (scope2.getTable() != null) {
                        if (scope2.hasKey(method_key)) {
                            Symbol var = scope2.get(method_key);
                            returnType = var.getType();
                            break;
                        }

                        scope2 = scope2.getParent();
                    }

                    if (!returnType.equals("Void") && !returnType.equals(""))
                        writeCode(TAB + "pop" + ENTER);
                }

            } else if (node.jjtGetChild(i) instanceof ASTArrayAccess) {
                generateCode((SimpleNode) node.jjtGetChild(i), global, scope);

            } else if (node.jjtGetChild(i) instanceof ASTIfStatement) {
                generateCode((SimpleNode) node.jjtGetChild(i), global, scope);
            } else if (node.jjtGetChild(i) instanceof ASTWhileStatement) {
                addWhileLabel();
                generateCode((SimpleNode) node.jjtGetChild(i).jjtGetChild(0), global, scope);
                addWhileCondition();
                generateCode((SimpleNode) node.jjtGetChild(i), global, scope);
                addEndWhileCondition();
            } else if (node.jjtGetChild(i) instanceof ASTCondition) {
                if ((node.jjtGetNumChildren() > i + 1) && (node.jjtGetChild(i + 1) instanceof ASTIfBody)) {
                    generateCode((SimpleNode) node.jjtGetChild(i), global, scope);
                    addIfCondition();
                }
            } else if (node.jjtGetChild(i) instanceof ASTLessThan) {
                generateCode((SimpleNode) node.jjtGetChild(i), global, scope);
                addLessThan();
            } else if (node.jjtGetChild(i) instanceof ASTIfBody) {
                generateCode((SimpleNode) node.jjtGetChild(i), global, scope);
                addEndIfCondition();
            } else if (node.jjtGetChild(i) instanceof ASTElseStatement) {
                generateCode((SimpleNode) node.jjtGetChild(i), global, scope);
                addEndElseCondition();
            } else if (node.jjtGetChild(i) instanceof ASTReturn) {
                generateCode((SimpleNode) node.jjtGetChild(i), global, scope);
            } else if (node.jjtGetChild(i) instanceof ASTVarDeclaration) {

                String typeName = ((SimpleNode) node.jjtGetChild(i).jjtGetChild(0)).toString();
                if (typeName == "VariableType")
                    typeName = ((SimpleNode) node.jjtGetChild(i).jjtGetChild(0)).jjtGetName().toString();

                String varName;
                SimpleNode nextNode = (SimpleNode) node.jjtGetChild(i).jjtGetChild(1);

                if (nextNode.toString() == "Array") {
                    typeName += "Array";
                    varName = ((SimpleNode) node.jjtGetChild(i).jjtGetChild(2)).jjtGetName().toString();
                } else {
                    varName = nextNode.jjtGetName().toString();
                }

                if (scope.equals("method"))
                    addVar(varName);
                else if (scope.equals("class")) {
                    addAttributeClass(varName, typeName);
                }
                if((node.jjtGetNumChildren() > (i+1)) && !(node.jjtGetChild(i+1) instanceof ASTVarDeclaration) && !(writeDefault.equals("method"))) {
                    addDefaultConstructor();
                }
            } else if (node.jjtGetChild(i) instanceof ASTLength) {
                if (localVariables.contains(((SimpleNode) node.jjtGetChild(i).jjtGetChild(0)).jjtGetName())) {
                    int index;
                    if (this.isStatic)
                        index = localVariables.indexOf(((SimpleNode) node.jjtGetChild(i).jjtGetChild(0)).jjtGetName());
                    else
                        index = localVariables.indexOf(((SimpleNode) node.jjtGetChild(i).jjtGetChild(0)).jjtGetName())
                                + 1;
                    String varType = analyzer.getTypeIdentifier((SimpleNode) node.jjtGetChild(i).jjtGetChild(0),
                            global);
                    String type = getDescriptor(varType);
                    if (type.equals("I") || type.equals("Z")) {
                        if (index > 3)
                            writeCode(TAB + "iload " + index + ENTER);
                        else
                            writeCode(TAB + "iload_" + index + ENTER);
                    } else {
                        if (index > 3)
                            writeCode(TAB + "aload " + index + ENTER);
                        else
                            writeCode(TAB + "aload_" + index + ENTER);

                    }
                } else if (attributes.containsKey(((SimpleNode) node.jjtGetChild(i).jjtGetChild(0)).jjtGetName())) {
                    writeCode(TAB + "aload_0" + ENTER);
                    writeCode(TAB + "getfield " + this.className + "/"
                            + ((SimpleNode) node.jjtGetChild(i).jjtGetChild(0)).jjtGetName() + " "
                            + getDescriptor(
                                    attributes.get(((SimpleNode) node.jjtGetChild(i).jjtGetChild(0)).jjtGetName()))
                            + ENTER);
                }
                writeCode(TAB + "arraylength" + ENTER);

            } else if (node.jjtGetChild(i) instanceof ASTNot) {
                generateCode((SimpleNode) node.jjtGetChild(i), global, scope);
               
                String message = TAB + "ifeq not_" + conditionCounter + ENTER;
                message += TAB + "iconst_0" + ENTER;
                message += TAB + "goto endCond_" + conditionCounter + ENTER;
                message += TAB + "not_" + conditionCounter + ":" + ENTER;
                message += TAB + "iconst_1" + ENTER;
                message += TAB + "endCond_" + conditionCounter + ":" + ENTER;
                writeCode(message);
                conditionCounter++;

                //writeCode(TAB + "ineg" + ENTER);
            }

        }

    }

    public void decArrayAccess(SimpleNode node) {
        for (int i = 0; i < node.jjtGetNumChildren(); i++) {
            if (node.jjtGetChild(i) instanceof ASTArrayAccess)
                decStack(1);
            decArrayAccess((SimpleNode) node.jjtGetChild(i));
        }
    }

    public int getStackLimit(SimpleNode node, SymbolTable scope) {
        for (int i = 0; i < node.jjtGetNumChildren(); i++) {
            if (node.jjtGetChild(i) instanceof ASTAssigned) {
                // incStack();
                getStackLimit((SimpleNode) node.jjtGetChild(i), scope);
                decStack(2);
                decArrayAccess((SimpleNode) node.jjtGetChild(i));

            } else if (node.jjtGetChild(i) instanceof ASTInteger) {
                incStack();
            } else if (node.jjtGetChild(i) instanceof ASTIndex) {
                getStackLimit((SimpleNode) node.jjtGetChild(i), scope);
            } else if (node.jjtGetChild(i) instanceof ASTTrue) {
                incStack();
            } else if (node.jjtGetChild(i) instanceof ASTFalse) {
                incStack();
            } else if (node.jjtGetChild(i) instanceof ASTAdd || node.jjtGetChild(i) instanceof ASTMultiply
                    || node.jjtGetChild(i) instanceof ASTSubtract || node.jjtGetChild(i) instanceof ASTDivide
                    || node.jjtGetChild(i) instanceof ASTAnd || node.jjtGetChild(i) instanceof ASTLessThan) {
                getStackLimit((SimpleNode) node.jjtGetChild(i), scope);
                decStack(1);
                decArrayAccess((SimpleNode) node.jjtGetChild(i));

            } else if (node.jjtGetChild(i) instanceof ASTNewInstance) {
                getStackLimit((SimpleNode) node.jjtGetChild(i), scope);
                // decArrayAccess((SimpleNode) node.jjtGetChild(i));
            } else if (node.jjtGetChild(i) instanceof ASTFunctionCall) {
                getStackLimit((SimpleNode) node.jjtGetChild(i).jjtGetChild(1), scope);

                String methodName = ((SimpleNode) node.jjtGetChild(i).jjtGetChild(0)).jjtGetName().toString();
                int numArg = node.jjtGetChild(i).jjtGetChild(1).jjtGetNumChildren();

                // get method key
                String method_key = "";
                method_key += methodName;
                method_key += "_" + numArg;

                for (int j = 0; j < numArg; j++)
                    method_key += "_" + analyzer
                            .getTypeIdentifier((SimpleNode) node.jjtGetChild(i).jjtGetChild(1).jjtGetChild(j), scope);

                String returnType = "";
                SymbolTable scope2 = scope;
                while (scope2.getTable() != null) {
                    if (scope2.hasKey(method_key)) {
                        Symbol var = scope2.get(method_key);
                        returnType = var.getType();
                        break;
                    }

                    scope2 = scope2.getParent();
                }

                int dec = 1 + numArg;

                if (returnType != "Void")
                    dec -= 1;

                decStack(dec);

                // pop
                if (node instanceof ASTMethod || node instanceof ASTIfBody || node instanceof ASTElseStatement
                        || node instanceof ASTWhileStatement) {
                    decStack(1);
                }

                decArrayAccess((SimpleNode) node.jjtGetChild(i));

            } else if (node.jjtGetChild(i) instanceof ASTThis) {
                incStack();
                getStackLimit((SimpleNode) node.jjtGetChild(i), scope);

                // pop
                if (node instanceof ASTMethod || node instanceof ASTIfBody || node instanceof ASTElseStatement
                        || node instanceof ASTWhileStatement)
                    decStack(1);

            } else if (node.jjtGetChild(i) instanceof ASTAssignExp) {
                getStackLimit((SimpleNode) node.jjtGetChild(i), scope);
            } else if (node.jjtGetChild(i) instanceof ASTIdentifier) {
                incStack();
            } else if (node.jjtGetChild(i) instanceof ASTVariable) {
                incStack();
            } else if (node.jjtGetChild(i) instanceof ASTValue) {
                getStackLimit((SimpleNode) node.jjtGetChild(i), scope);

                // pop
                if ((node instanceof ASTMethod || node instanceof ASTIfBody || node instanceof ASTElseStatement
                        || node instanceof ASTWhileStatement) && node.jjtGetChild(i).jjtGetNumChildren() > 1
                        && node.jjtGetChild(i).jjtGetChild(1).toString().equals("FunctionCall")) {
                    decStack(1);
                }

            } else if (node.jjtGetChild(i) instanceof ASTArrayAccess) {
                getStackLimit((SimpleNode) node.jjtGetChild(i), scope);
            } else if (node.jjtGetChild(i) instanceof ASTIfStatement) {
                getStackLimit((SimpleNode) node.jjtGetChild(i), scope);
            } else if (node.jjtGetChild(i) instanceof ASTWhileStatement) {
                getStackLimit((SimpleNode) node.jjtGetChild(i), scope);
            } else if (node.jjtGetChild(i) instanceof ASTCondition) {
                getStackLimit((SimpleNode) node.jjtGetChild(i), scope);
                decStack(1);
            } else if (node.jjtGetChild(i) instanceof ASTIfBody) {
                getStackLimit((SimpleNode) node.jjtGetChild(i), scope);
            } else if (node.jjtGetChild(i) instanceof ASTElseStatement) {
                getStackLimit((SimpleNode) node.jjtGetChild(i), scope);
            } else if (node.jjtGetChild(i) instanceof ASTReturn) {
                getStackLimit((SimpleNode) node.jjtGetChild(i), scope);
                decStack(1);
            } else if (node.jjtGetChild(i) instanceof ASTLength) {
                incStack();
                getStackLimit((SimpleNode) node.jjtGetChild(i), scope);

            } else if (node.jjtGetChild(i) instanceof ASTNot) {
                getStackLimit((SimpleNode) node.jjtGetChild(i), scope);
            }

        }
        return maxStack;
    }

    public void incStack() {
        totalStack++;
        if (totalStack > maxStack) {
            maxStack = totalStack;
        }
    }

    public void decStack(int value) {
        totalStack -= value;
    }

    public void writeCode(String content) {
        try {
            fileWriter.write(content);
            fileWriter.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getDescriptor(String type) {
        if (type.equals("Int") || type.equals("Integer")) {
            return "I";
        } else if (type.equals("Boolean")) {
            return "Z";
        } else if (type.equals("IntArray")) {
            return "[I";
        } else if (type.equals("Void") || type.equals("") || type.equals("VOID")) {
            return "V";
        }

        return "L" + type + ";";
    }

    public void addClass(String name, String superClassName) {
        writeCode(".class public " + name + ENTER);
        writeCode(".super ");

        this.className = name;

        if (superClassName.equals("Object"))
            writeCode("java/lang/");

        writeCode(superClassName + ENTER);

       
    }

    public void addDefaultConstructor() {
        // add default constructor
        String constructor = ".method public <init>()V" + ENTER;

        constructor += TAB + "aload_0" + ENTER;
        constructor += TAB + "invokenonvirtual ";

        if (this.extendsName == "Object")
            constructor += "java/lang/";

        constructor += this.extendsName + "/<init>()V" + ENTER;
        constructor += TAB + "return" + ENTER;
        constructor += ".end method" + ENTER;

        writeCode(constructor);
    }
    public void addAttributeClass(String varName, String varType) {
        String attributeDeclaration = ".field public " + varName + " ";
        attributeDeclaration += getDescriptor(varType);
        writeCode(attributeDeclaration + ENTER);
        attributes.putIfAbsent(varName, varType);
    }

    public void addMain(SimpleNode node, SymbolTable symbolTable) {
        localVariables = new ArrayList<String>();
        this.isStatic = true;
        this.conditionCounter = 0;
        writeCode(".method public static main([Ljava/lang/String;)V" + ENTER);
        int stackLimit = getStackLimit(node, symbolTable);
        maxStack = 0;
        totalStack = 0;
        writeCode(TAB + ".limit stack " + stackLimit + ENTER);
        int numVars = symbolTable.getTable().size();
        writeCode(TAB + ".limit locals " + numVars + ENTER);

        for (AbstractMap.SimpleEntry<String, Symbol> param : symbolTable.getParams()) {
            localVariables.add(param.getKey());
        }
    }

    public void addMethod(SimpleNode node, SymbolTable symbolTable, String methodName) {
        localVariables = new ArrayList<String>();
        this.isStatic = false;
        this.conditionCounter = 0;

        String declaration = ".method public " + methodName + "(";

        String loadParams = "";
        int i = 1;

        for (AbstractMap.SimpleEntry<String, Symbol> param : symbolTable.getParams()) {

            String param_type = param.getValue().getType();

            String type = getDescriptor(param_type);
            declaration += type;

            localVariables.add(param.getKey());
            i++;
        }

        declaration += ")";

        declaration += getDescriptor(symbolTable.getReturn().getType()) + ENTER;

        int stackLimit = getStackLimit(node, symbolTable);
        maxStack = 0;
        totalStack = 0;
        declaration += TAB + ".limit stack " + stackLimit + ENTER;
        int numVars = symbolTable.getTable().size() + 1;
        declaration += TAB + ".limit locals " + numVars + ENTER;

        writeCode(declaration);
        // writeCode(loadParams + ENTER);

    }

    public void endMethod(SymbolTable symbolTable) {
        String message = "";
        if (symbolTable.getReturn().getType() == "Static_Void_Main")
            message += TAB + "return" + ENTER;
        else if (symbolTable.getReturn().getType().equals("Int") || symbolTable.getReturn().getType().equals("Integer")
                || symbolTable.getReturn().getType().equals("Boolean"))
            message += TAB + "ireturn" + ENTER;
        else
            message += TAB + "areturn" + ENTER;

        message += ".end method" + ENTER;
        writeCode(message);
    }

    public void addVar(String varName) {
        localVariables.add(varName);
    }

    public void addAssign(String type, String name) {
        String line = "";
        if (localVariables.contains(name)) {
            int index;
            if (this.isStatic) {
                index = localVariables.indexOf(name);
            } else
                index = localVariables.indexOf(name) + 1;
            if (type.equals("Int") || type.equals("Integer") || type.equals("Boolean")) {
                line += TAB + "istore " + index + ENTER;
            }
            // else if (type.equals("IntArray") || type.equals("StringArray")) {
            // line += TAB + "iastore" + ENTER + ENTER;
            // }
            else {
                line += TAB + "astore " + index + ENTER;
            }
        } else {
            line += TAB + "putfield " + this.className + "/" + name + " " + getDescriptor(type) + ENTER;
        }
        writeCode(line);
    }

    public void closeFile() {
        try {
            fileWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addArithmeticExp(SimpleNode node) {
        if (node instanceof ASTDivide) {
            writeCode(TAB + "idiv" + ENTER);
        } else if (node instanceof ASTAdd) {
            writeCode(TAB + "iadd" + ENTER);
        } else if (node instanceof ASTSubtract) {
            writeCode(TAB + "isub" + ENTER);
        } else if (node instanceof ASTMultiply) {
            writeCode(TAB + "imul" + ENTER);
        }
    }

    public void addInteger(int value) {
        String line = "";
        if (value >= 0 && value <= 5) {
            line += TAB + "iconst_" + value + ENTER;
        } else if (value >= -127 && value <= 128) {
            line += TAB + "bipush " + value + ENTER;
        } else if (value >= -32768 && value <= 32767) {
            line += TAB + "sipush " + value + ENTER;
        } else {
            line += TAB + "ldc " + value + ENTER;
        }

        writeCode(line);
    }

    public void staticInvocation(String parent, SimpleNode invoke, String[] types, String returnType,
            String scope, SymbolTable global) {
        String invocation = "";
        int i = 0;
 
        String type = getDescriptor(returnType);
        invocation = TAB + "invokestatic " + parent + "/" + ((SimpleNode) invoke.jjtGetChild(0)).jjtGetName().toString()
                + "(";

        for (String param : types) {
            invocation += getDescriptor(param);
        }
        invocation += ")" + type + ENTER;
        writeCode(invocation);
    }

    public void functionInvocation(String parent, SimpleNode invoke, String[] types,
            String returnType, String scope, SymbolTable global) {
        String invocation = "";
        int i = 0;
      
        String type = getDescriptor(returnType);

        invocation += TAB + "invokevirtual " + parent + "/"
                + ((SimpleNode) invoke.jjtGetChild(0)).jjtGetName().toString() + "(";

        for (String param : types) {
            invocation += getDescriptor(param);
        }
        invocation += ")" + type + ENTER;

        writeCode(invocation);
    }

    public void invokeNewInstance(String classInstance) {
        String invocation = TAB + "new " + classInstance + ENTER + TAB + "dup" + ENTER;
        invocation += TAB + "invokespecial " + classInstance + "/<init>()V" + ENTER;
        writeCode(invocation);
    }

    public void addLessThan() {
        String message = TAB + "if_icmpge else_" + conditionCounter + ENTER;
        message += TAB + "iconst_1" + ENTER;
        message += TAB + "goto endCond_" + conditionCounter + ENTER;
        message += TAB + "else_" + conditionCounter + ":" + ENTER;
        message += TAB + "iconst_0" + ENTER;
        message += TAB + "endCond_" + conditionCounter + ":" + ENTER;
        writeCode(message);
        conditionCounter++;
    }

    public void addAnd() {
        writeCode(TAB + "iand" + ENTER);
    }

    public void addIfCondition() {
        String message = TAB + "iconst_1" + ENTER;
        message += TAB + "if_icmpne else_" + conditionCounter + ENTER;
        conditionDepth.push(conditionCounter);
        conditionCounter++;
        writeCode(message);
    }

    public void addEndIfCondition() {
        int condCount = conditionDepth.pop();
        String message = TAB + "goto endCond_" + condCount + ENTER;
        message += TAB + "else_" + condCount + ":" + ENTER;
        conditionDepth.push(condCount);
        writeCode(message);
    }

    public void addEndElseCondition() {
        int condCount = conditionDepth.pop();
        writeCode(TAB + "endCond_" + condCount + ":" + ENTER);
    }

    public void addWhileLabel() {
        writeCode(TAB + "while_" + conditionCounter + ":" + ENTER);
        conditionDepth.push(conditionCounter);
        conditionCounter++;
    }

    public void addWhileCondition() {
        int condCount = conditionDepth.pop();
        String message = TAB + "iconst_1" + ENTER;
        message += TAB + "if_icmpne endCond_" + condCount + ENTER;
        conditionDepth.push(condCount);
        writeCode(message);
    }

    public void addEndWhileCondition() {
        int condCount = conditionDepth.pop();
        String message = TAB + "goto while_" + condCount + ENTER;
        message += TAB + "endCond_" + condCount + ":" + ENTER;
        writeCode(message);
    }
}