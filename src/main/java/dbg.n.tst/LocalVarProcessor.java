package dbg.n.tst;

import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.AnnotationMirror;
import javax.tools.Diagnostic;

import com.sun.source.util.Trees;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.tree.VariableTree;
import com.sun.source.tree.Tree;
import static basix_funx._basix_funx.prnt;
import static dbg.n.tst.marker.getAnnValue;
@SupportedAnnotationTypes("*")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class LocalVarProcessor extends AbstractProcessor {
    private Trees trees;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        trees = Trees.instance(processingEnv);
    }

    @Override
    public boolean process(Set<? extends javax.lang.model.element.TypeElement> annotations,
                           RoundEnvironment roundEnv) {
        for (Element root : roundEnv.getRootElements()) {
            TreePath path = trees.getPath(root);
            if (path == null) continue;
            new TreePathScanner<Void, Void>() {
                @Override
                public Void visitVariable(VariableTree node, Void p) {
                    TreePath current = getCurrentPath();
                    Element elem = trees.getElement(current);
                    if (elem != null && elem.getKind() == ElementKind.LOCAL_VARIABLE) {
                        for (AnnotationMirror am : elem.getAnnotationMirrors()) {
                            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                                    "Found local annotation: " + am, elem);
                                    prnt ("tst am: " + getAnnValue ( processingEnv, am, "STRING" ) );
                        }
                    }
                    return super.visitVariable(node, p);
                }
            }.scan(path, null);
        }
        return false; // allow other processors to run
    }
}
/*
How to use:

    Compile and put this processor on the annotation-processing path (javac -processorpath ...).
 */