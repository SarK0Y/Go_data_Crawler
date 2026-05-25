package dbg.n.tst;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.lang.Class;
import java.util.ArrayList;
import java.util.List;
import Anns.tst;
public class dbg <T> {
    public List <Method> find_marked_methods (T _class, Class mark) {
        List <Method> ret = new ArrayList <> ();
        for (Method fn: _class.getClass().getDeclaredMethods()) {
            if (fn.isAnnotationPresent (mark)){
                ret.add (fn);
            }
        }
        return ret;
    }
    public List <String> names_of_marked_methods (List <Method> marked_ones) {
        List <String> ret = new ArrayList <> ();
        for (Method name: marked_ones) {
            ret.add(name.getName());
        }
        return ret;
    }
} 
/*
public List<String> namesOfMarkedMethods(List<Method> marks) {
    List<String> ret = new ArrayList<>();
    for (Method m : marks) {
        ret.add(m.getDeclaringClass().getCanonicalName());
    }
    return ret;
}
////////////////////////
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.BodyDeclaration;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

Path path = Path.of("src/main/java/com/example/MyClass.java");
CompilationUnit cu = JavaParser.parse(path);

// get the first class or a specific one by name
Optional<ClassOrInterfaceDeclaration> clsOpt = cu.findFirst(ClassOrInterfaceDeclaration.class,
    c -> c.getNameAsString().equals("MyClass"));

if (clsOpt.isPresent()) {
    ClassOrInterfaceDeclaration cls = clsOpt.get();

    // 1) Get members (methods, fields, constructors, initializers)
    List<BodyDeclaration<?>> members = cls.getMembers();

    // 2) Get source text of the class body (between braces)
    String bodyText = cls.getMembers().stream()
        .map(Object::toString)
        .reduce((a,b) -> a + System.lineSeparator() + b)
        .orElse("");

    // 3) Or get the whole class source (including header)
    String classSource = cls.toString();
}
Notes:

    Use cu.getTypes() or cu.getPrimaryType() if you prefer indexed access.
    For nested or multiple classes, filter by name or iterate cu.findAll(ClassOrInterfaceDeclaration.class).
    To preserve formatting/comments, consider using JavaParser's LexicalPreservingPrinter:
        Wrap: LexicalPreservingPrinter.setup(cu)
        Then use LexicalPreservingPrinter.print(cls) to get preserved source.

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import java.nio.file.Path;
import java.util.Optional;

Path path = Path.of("src/main/java/com/example/MyClass.java");
CompilationUnit cu = JavaParser.parse(path);

// enable lexical preserving
LexicalPreservingPrinter.setup(cu);

// locate class
Optional<ClassOrInterfaceDeclaration> clsOpt = cu.findFirst(ClassOrInterfaceDeclaration.class,
    c -> c.getNameAsString().equals("MyClass"));

if (clsOpt.isPresent()) {
    ClassOrInterfaceDeclaration cls = clsOpt.get();

    // Example modification: add a method
    cls.addMethod("hello").setType("void").setBody(
        JavaParser.parseBlock("{ System.out.println(\"hi\"); }"));

    // print with original formatting/comments preserved
    String result = LexicalPreservingPrinter.print(cu);
    System.out.println(result);
}

    Tips and pitfalls

    Always call LexicalPreservingPrinter.setup(cu) immediately after parsing; otherwise preservation won't work reliably.
    When creating new nodes, prefer using JavaParser.parseX helpers (parseBlock, parseStatement) or build nodes via API; manually-crafted toString() nodes won't have original token ranges.
    For single-node printing (e.g., print only the class), use: String classText = LexicalPreservingPrinter.print(cls); (works after setup)
    Replacing/removing nodes: use node.replace(...) or remove(); LexicalPreservingPrinter keeps surrounding whitespace/comments.
    Beware of using JavaParser.parse(...) again on subtrees — that creates new CompilationUnits not hooked to the lexical printer.
    Some complex formatting (aligned comments, column-based alignment) may not be perfectly preserved; test on real files.

    Updating file

    After printing the modified CU, write back to file (overwrite) to persist changes.
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import java.nio.file.Path;
import java.util.Optional;

Path path = Path.of("src/main/java/com/example/MyClass.java");
CompilationUnit cu = JavaParser.parse(path);
LexicalPreservingPrinter.setup(cu);

// find method by name (or add additional filters for params/annotations)
Optional<MethodDeclaration> mOpt = cu.findFirst(MethodDeclaration.class,
    m -> m.getNameAsString().equals("myMethod"));

if (mOpt.isPresent()) {
    MethodDeclaration m = mOpt.get();

    // 1) Replace entire body with a parsed block
    m.setBody(JavaParser.parseBlock("{ System.out.println(\"new body\"); }"));

    // 2) Or modify statements inside existing body:
    // m.getBody().ifPresent(body -> {
    //     body.getStatements().clear();
    //     body.addStatement("System.out.println(\"replaced\");");
    // });

    // 3) Or change signature (return type, params, modifiers)
    // m.setType("int");
    // m.setName("newName");
    // m.addParameter("String", "arg");

    // Print whole CU (preserves original formatting/comments)
    String updated = LexicalPreservingPrinter.print(cu);
    System.out.println(updated);
    // write back to file if desired
}
Notes:

    Call LexicalPreservingPrinter.setup(cu) immediately after parsing.
    Use JavaParser.parseBlock / parseStatement to create proper nodes so token ranges are tracked.
    For adding statements to existing body prefer body.addStatement(...) rather than manipulating strings.
    To replace a method entirely, you can use m.replace(newMethodDeclaration).
    After changes, write LexicalPreservingPrinter.print(cu) back to the source file to persist
 */