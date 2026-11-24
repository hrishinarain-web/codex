
// package com.analyzer.parser;

// import org.eclipse.jdt.core.dom.AST;
// import org.eclipse.jdt.core.dom.ASTParser;
// import org.eclipse.jdt.core.dom.ASTVisitor;
// import org.eclipse.jdt.core.dom.CompilationUnit;
// import org.eclipse.jdt.core.dom.FieldDeclaration;
// import org.eclipse.jdt.core.dom.IMethodBinding;
// import org.eclipse.jdt.core.dom.ITypeBinding;
// import org.eclipse.jdt.core.dom.MethodDeclaration;
// import org.eclipse.jdt.core.dom.MethodInvocation;
// import org.eclipse.jdt.core.dom.TypeDeclaration;

// public class JavaFileParser {

//     public void parse(String sourceCode) {

//         ASTParser parser = ASTParser.newParser(AST.getJLSLatest());

//         parser.setSource(sourceCode.toCharArray());

//         parser.setKind(ASTParser.K_COMPILATION_UNIT);

//         CompilationUnit cu =
//                 (CompilationUnit) parser.createAST(null);



//         // ASTParser parser = ASTParser.newParser(AST.getJLSLatest());

//         // parser.setKind(ASTParser.K_COMPILATION_UNIT);

//         // parser.setSource(source.toCharArray());

//         // parser.setResolveBindings(true);
//         // parser.setBindingsRecovery(true);

//         // parser.setEnvironment(
//         //     classpathEntries,
//         //     sourcepathEntries,
//         //     encodings,
//         //     true
//         // );

// // parser.setUnitName("OrderService.java");

// // CompilationUnit cu =
// //     (CompilationUnit) parser.createAST(null);
//         // Package
//         if (cu.getPackage() != null) {
//             System.out.println(
//                 "Package: " + cu.getPackage().getName()
//             );
//         }

//         // Visit every declaration in the source
//         cu.accept(new ASTVisitor() {

//             @Override
//             public boolean visit(TypeDeclaration node) {

//                 System.out.println("\nClass: " + node.getName());

//                 if (node.getSuperclassType() != null) {
//                     System.out.println(
//                         "Extends: " + node.getSuperclassType()
//                     );
//                 }

//                 for (Object type : node.superInterfaceTypes()) {
//                     System.out.println("Implements: " + type);
//                 }

//                 return true;
//             }

//             @Override
//             public boolean visit(FieldDeclaration node) {

//                 System.out.println("Field: " + node);

//                 return true;
//             }

//             @Override
//             public boolean visit(MethodDeclaration node) {

//                 System.out.println(
//                     "Method: " + node.getName()
//                 );

//                 System.out.println(
//                     "Return type: " + node.getReturnType2()
//                 );

//                 return true;
//             }


//             @Override
//             public boolean visit(MethodInvocation node) {

//                 String methodName = node.getName().getIdentifier();

//                 IMethodBinding binding = node.resolveMethodBinding();

//                 if (binding != null) {
//                     ITypeBinding declaringClass = binding.getDeclaringClass();

//                     String className = declaringClass.getQualifiedName();

//                     System.out.println(
//                         "Calling: " + className + "." + methodName
//                     );
//                 } else {
//                     System.out.println(
//                         "Calling: UNKNOWN." + methodName
//                     );
//                 }

//                 return true;
//             }

//         });
//     }
// }



package com.analyzer.parser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.TypeDeclaration;

public class JavaFileParser {

    private static final String PROJECT_ROOT = "/app";

    private static final String SOURCE_ROOT =
            PROJECT_ROOT + "/src/main/java";

    private static final String OUTPUT_DIR =
            PROJECT_ROOT + "/target/classes";

    private static final String DEPENDENCY_FILE =
            PROJECT_ROOT + "/target/dependency-classpath.txt";


    // Existing API: parse source code directly.
    // Attempts to infer the compilation unit name.
    public void parse(String sourceCode) {

        String unitName = inferUnitName(sourceCode);

        parse(sourceCode, unitName);
    }


    // Preferred API: pass the actual relative file path.
    public void parse(String sourceCode, String relativeFilePath) {

        ASTParser parser = ASTParser.newParser(AST.getJLSLatest());

        parser.setKind(ASTParser.K_COMPILATION_UNIT);

        parser.setSource(sourceCode.toCharArray());

        // Enable binding resolution
        parser.setResolveBindings(true);
        parser.setBindingsRecovery(true);
        parser.setStatementsRecovery(true);

        // Java 21 compiler configuration
        Map<String, String> options = JavaCore.getOptions();

        JavaCore.setComplianceOptions(
                JavaCore.VERSION_21,
                options
        );

        parser.setCompilerOptions(options);


        // Load Maven dependencies
        String[] dependencies = loadDependencies();

        // Add compiled project classes to classpath
        String[] classpath = new String[dependencies.length + 1];

        classpath[0] = OUTPUT_DIR;

        System.arraycopy(
                dependencies,
                0,
                classpath,
                1,
                dependencies.length
        );


        // Source roots
        String[] sourcepath = {
                SOURCE_ROOT
        };

        String[] encodings = {
                "UTF-8"
        };


        // Configure JDT environment
        parser.setEnvironment(
                classpath,
                sourcepath,
                encodings,
                true
        );

        // Important: relative path from source root
        parser.setUnitName(relativeFilePath);


        // Create AST
        CompilationUnit cu =
                (CompilationUnit) parser.createAST(null);


        // Print compilation errors
        for (IProblem problem : cu.getProblems()) {

            if (problem.isError()) {

                System.out.println(
                        "JDT ERROR: " + problem.getMessage()
                );
            }
        }


        // Package
        if (cu.getPackage() != null) {

            System.out.println(
                    "Package: " + cu.getPackage().getName()
            );
        }


        // Traverse AST
        cu.accept(new ASTVisitor() {

            @Override
            public boolean visit(TypeDeclaration node) {

                System.out.println(
                        "\nClass: " + node.getName()
                );

                if (node.getSuperclassType() != null) {

                    System.out.println(
                            "Extends: " + node.getSuperclassType()
                    );
                }

                for (Object type : node.superInterfaceTypes()) {

                    System.out.println(
                            "Implements: " + type
                    );
                }

                return true;
            }


            @Override
            public boolean visit(FieldDeclaration node) {

                System.out.println(
                        "Field: " + node
                );

                return true;
            }


            @Override
            public boolean visit(MethodDeclaration node) {

                System.out.println(
                        "Method: " + node.getName()
                );

                System.out.println(
                        "Return type: " + node.getReturnType2()
                );

                return true;
            }


            @Override
            public boolean visit(MethodInvocation node) {

                String methodName =
                        node.getName().getIdentifier();

                IMethodBinding binding =
                        node.resolveMethodBinding();

                if (binding != null) {

                    ITypeBinding declaringClass =
                            binding.getDeclaringClass();

                    String className =
                            declaringClass != null
                                    ? declaringClass.getQualifiedName()
                                    : "UNKNOWN";

                    System.out.println(
                            "Calling: " + className + "." + methodName
                    );

                    // Print method signature
                    System.out.println(
                            "  Signature: " + binding.toString()
                    );

                } else {

                    System.out.println(
                            "Calling: UNKNOWN." + methodName
                    );

                    // Useful debugging information
                    System.out.println(
                            "  Receiver: " +
                            (node.getExpression() != null
                                    ? node.getExpression()
                                    : "implicit receiver")
                    );
                }

                return true;
            }

        });
    }


    // Read Maven dependency classpath
    private String[] loadDependencies() {

        try {

            Path dependencyPath = Path.of(DEPENDENCY_FILE);

            if (!Files.exists(dependencyPath)) {

                System.out.println(
                        "WARNING: Maven dependency classpath not found."
                );

                return new String[0];
            }

            String dependencyClasspath =
                    Files.readString(dependencyPath).trim();

            if (dependencyClasspath.isEmpty()) {
                return new String[0];
            }

            return dependencyClasspath.split(
                    Pattern.quote(
                            System.getProperty("path.separator")
                    )
            );

        } catch (IOException e) {

            throw new RuntimeException(
                    "Failed to load Maven dependency classpath",
                    e
            );
        }
    }


    // Infer package and top-level type to generate a unit name.
    // Passing the actual relative path is preferred.
    private String inferUnitName(String sourceCode) {

        String packageName = "";

        Matcher packageMatcher = Pattern.compile(
                "\\bpackage\\s+([\\w.]+)\\s*;"
        ).matcher(sourceCode);

        if (packageMatcher.find()) {
            packageName = packageMatcher.group(1);
        }

        Matcher classMatcher = Pattern.compile(
                "\\b(?:public\\s+)?(?:abstract\\s+|final\\s+)?"
                + "(?:class|interface|enum|record)\\s+(\\w+)"
        ).matcher(sourceCode);

        if (!classMatcher.find()) {
            return "Unknown.java";
        }

        String className = classMatcher.group(1);

        String packagePath = packageName.isEmpty()
                ? ""
                : packageName.replace('.', '/') + "/";

        return packagePath + className + ".java";
    }
}
