# Java-- Compiler

## GROUP 5a

João Alberto Preto Rodrigues Praça, 201704748, 18, 28
Lucas Tomás Martins Ribeiro, 201705227, 16.5, 20
Paulo Jorge Palhau Moutinho, 201704710, 16.5, 20
Sílvia Jorge Moreira da Rocha, 201704684, 18.4,  32 

### Project 


Global Grade: 17

#### Summary

In this project we intended to apply the skills we developed during our Compilers class towards the development of a compiler for programs in Java. This should be accomplished trough the generation of valid Jasmin instructions, which is a tool that would generate the desired Java Bytecodes.  

#### Execute

To compile the code of our tool you must use the following command:
>gradle build

Then to execute our tool you must use these commands:
>java -jar .\comp2020-5a.jar <filename>.jmm

>java -jar .\jasmin.jar jasminCode/<filename>.j

>java <filename>


#### Handling syntax errors

Our tool is detecting the first 5 syntax errors, displaying them and exiting afterwards. When our tool finds a syntax error it will simply skip until the next relevant portion of the code, per example:
        -If the tool finds a error in a while expression it will skip until the next closing parenthesis.


#### Semantic Analysis

We only verify the compliance of the semantic rules in the expressions and function and method calls of our code, which is what's required by our project specification.
This includes:
        -We verify that when a variable is assigned to another variable, they are both of the same type.
        -We verify that operations only occur between variables of the same type. Including the verification of Array Accesses.
        -We verify that conditional expressions result in boleans.
        -We check that a variable is only used if it has already been initialized. Taking into account the If, While and For scopes.
        -We verify that a method is only used following the correct target object or class. This both for the declared function and the imported ones.
        -We  verify if the numbers of arguments in a method or function call is the same as in its declaration. The same applies for the types of these arguments. This also takes into account method overloading.

#### Code Generation
We take into account the AST as the starting point for our code generation and then proceed to generate the corresponding jasmin code. During this process we caclculate the stack limit byt taking into account the push and pop instructions to obtain the maximum value it can reach for each method. To calculate the local limit we take into account the number of local variable plus the number of parameters in case this method is static, otherwise we increment the value to take into account the reference to the current object ("this") which takes the bottom of the stack.

#### Overview
This tool compiles a Java file: it starts by reading the file, parsing each token and creating an abstract syntax tree; then, this tree is analyzed semmantically, tokens are converted into symbols and the file, class and method's symbol tables are created; lastly, using the file's symbol table, the jasmin code is generated, which is then used to create a runnable class file.

#### Task Distribution

| Task                                          | Member                                                  | 
|---------------------------------------------- |-------------------------------------------------------- |
| Translation of the grammar into tokens        | Paulo Moutinho, Sílvia Rocha, João Praça                |
| Syntatic Analysis                             | Paulo Moutinho, Sílvia Rocha, João Praça, Lucas Ribeiro |
| Error Handling                                | Sílvia Rocha, João Praça, Lucas Ribeiro                 |
| Abstract Syntax Tree                          | Sílvia Rocha                                            | 
| Symbol Tables                                 | Sílvia Rocha, João Praça, Lucas Ribeiro                 |
| Semantic Analysis                             | Paulo Moutinho, Sílvia Rocha, João Praça, Lucas Ribeiro |
| Stack size calculation                        | João Praça, Sílvia Rocha                                |
| Code Generation for function calls            | João Praça, Sílvia Rocha                                |
| Code Generation for arithmetic expressions    | Sílvia Rocha                                            |
| Code Generation for conditions                | João Praça, Sílvia Rocha                                |
| Code Generation for loops                     | João Praça, Sílvia Rocha                                |
| Code Generation for arrays                    | João Praça, Sílvia Rocha                                |


#### Analysis of the strengths and weaknesses of the tool
PROS: We developed a feature that indicates wether or not there is the possibility that one of the referenced variables hasn't been initialized.
CONS: The optimizations aren't fully implemented.