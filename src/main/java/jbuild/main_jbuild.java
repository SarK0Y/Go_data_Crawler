package jbuild;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.Modifier;
import java.util.Optional;
import jbuild.ifc_jbuild;
import Anns.tst;
public class main_jbuild implements ifc_jbuild {
    private String url;
    private Optional <String> alt_src;
    private String YourCode;
    private boolean changed;
    public main_jbuild (String url, String src, String YourCode) {
        this.alt_src = Optional.ofNullable (src);
        this.url = url;
        this.YourCode = YourCode;
        this.changed = false;
    }
    public static Optional <String> add_code_to_src (String src, String YourCode) {
        @tst
        var x = 4;
        return Optional.empty();
    }
    @Override
    public Optional <String> add_code_to_src () {
        return Optional.empty();
    }
    @Override
    public void update_src () {
    }
}