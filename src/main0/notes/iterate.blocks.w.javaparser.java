package notes;
/*
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.utils.SourceRoot;

import java.nio.file.Paths;

public class IterateStatements {
    public static void main(String[] args) throws Exception {
        String code = """
            class C {
                void foo() {
                    int a = 1;
                    if (a > 0) {
                        a++;
                    } else {
                        a--;
                    }
                    for (int i=0;i<3;i++) System.out.println(i);
                    return;
                }
            }
            """;

        CompilationUnit cu = JavaParser.parse(code);

        // find method by name
        cu.findAll(MethodDeclaration.class).stream()
          .filter(m -> m.getNameAsString().equals("foo"))
          .forEach(m -> {
              m.getBody().ifPresent(body -> {
                  // top-level statements in the method block
                  for (Statement stmt : body.getStatements()) {
                      System.out.println("Statement: " + stmt.getClass().getSimpleName());
                      System.out.println(stmt); // full source for the statement

                      // Example: handle specific statement kinds
                      stmt.ifIfStmt(ifStmt -> {
                          System.out.println("  It's an if with condition: " + ifStmt.getCondition());
                      });
                      stmt.ifForStmt(forStmt -> {
                          System.out.println("  It's a for loop: " + forStmt.getCompare());
                      });
                      stmt.ifExpressionStmt(exprStmt -> {
                          System.out.println("  ExprStmt: " + exprStmt.getExpression());
                      });
                  }
              });
          });
    }
}
Notes:

    Use getBody().ifPresent(...) because methods can be abstract or interface declarations.
    body.getStatements() returns only top-level statements in the method block. To traverse nested statements (inside if/for/while/blocks), either:
        Recursively inspect child Statement nodes (e.g., inspect IfStmt.getThenStmt()/getElseStmt(), BlockStmt.getStatements(), ForStmt.getBody(), etc.), or
        Use a visitor: extend VoidVisitorAdapter or use ModifierVisitor to visit all Statement nodes:
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

public class IterateNestedStatements {
    public static void main(String[] args) {
        String code = """
            class C {
                void foo() {
                    int a = 1;
                    if (a > 0) {
                        a++;
                        for (int i = 0; i < 2; i++) {
                            System.out.println(i);
                            { int x = 5; x++; }
                        }
                    } else {
                        a--;
                    }
                    while (a < 10) a++;
                }
            }
            """;

        CompilationUnit cu = JavaParser.parse(code);

        cu.findAll(MethodDeclaration.class).stream()
          .filter(m -> m.getNameAsString().equals("foo"))
          .forEach(m -> m.getBody().ifPresent(body -> {
              for (Statement stmt : body.getStatements()) {
                  visitStatement(stmt, 0);
              }
          }));
    }

    static void printIndent(int depth) {
        System.out.print("  ".repeat(depth));
    }

    static void visitStatement(Statement stmt, int depth) {
        printIndent(depth);
        System.out.println(stmt.getClass().getSimpleName() + ": " + stmt);

        // Handle blocks directly
        if (stmt.isBlockStmt()) {
            BlockStmt block = stmt.asBlockStmt();
            for (Statement child : block.getStatements()) {
                visitStatement(child, depth + 1);
            }
            return;
        }

        // If statements: then + else (else can be another IfStmt or a BlockStmt or single stmt)
        stmt.ifIfStmt(ifStmt -> {
            visitStatement(ifStmt.getCondition().asExpressionStmt(), depth + 1); // optional: show condition wrapper
            visitStatement(ifStmt.getThenStmt(), depth + 1);
            ifStmt.getElseStmt().ifPresent(elseStmt -> visitStatement(elseStmt, depth + 1));
        });

        // For, Foreach, While, DoWhile: visit body (body may be BlockStmt or single statement)
        stmt.ifForStmt(forStmt -> visitStatement(forStmt.getBody(), depth + 1));
        stmt.ifForEachStmt(forEach -> visitStatement(forEach.getBody(), depth + 1));
        stmt.ifWhileStmt(whileStmt -> visitStatement(whileStmt.getBody(), depth + 1));
        stmt.ifDoStmt(doStmt -> visitStatement(doStmt.getBody(), depth + 1));

        // Try-catch-finally: visit try block, catch clauses, and finally block
        stmt.ifTryStmt(tryStmt -> {
            visitStatement(tryStmt.getTryBlock(), depth + 1);
            tryStmt.getCatchClauses().forEach(c -> visitStatement(c.getBody(), depth + 1));
            tryStmt.getFinallyBlock().ifPresent(f -> visitStatement(f, depth + 1));
        });

        // Switch: visit entries' statements
        stmt.ifSwitchStmt(sw -> {
            sw.getEntries().forEach(e -> e.getStatements().forEach(s -> visitStatement(s, depth + 1)));
        });

        // Synchronized: visit body
        stmt.ifSynchronizedStmt(sync -> visitStatement(sync.getBody(), depth + 1));

        // LabeledStmt: visit inner statement
        stmt.ifLabeledStmt(lbl -> visitStatement(lbl.getStatement(), depth + 1));

        // ExpressionStmt, ReturnStmt, BreakStmt, ContinueStmt, EmptyStmt: leaf nodes
    }
}
Notes:

    The code treats many composite statement types explicitly (IfStmt, ForStmt, WhileStmt, TryStmt, SwitchStmt, etc.).

cu.findAll(MethodDeclaration.class).forEach(m ->
    m.accept(new VoidVisitorAdapter<Void>() {
        @Override
        public void visit(Statement s, Void arg) {
            super.visit(s, arg);
            System.out.println(s.getClass().getSimpleName() + ": " + s);
        }
    }, null)
Use the recursive approach when you need control over traversal order or to perform transformations; use the visitor when you only need to inspect all statements.
);

 */