package dbg.n.tst;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.lang.Class;
import java.util.ArrayList;
import java.util.List;
import Anns.Mark;
import java.lang.annotation.Annotation;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Name;
import java.util.Map;
import java.util.Optional;
import static basix_funx._basix_funx.prnt;
public class marker {
    static Optional <Object> getAnnValue (ProcessingEnvironment processingEnv, AnnotationMirror am, String keyName) {
        Map<? extends ExecutableElement, ? extends AnnotationValue> values = processingEnv.getElementUtils()
       .getElementValuesWithDefaults(am);
        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : values.entrySet()) {
            ExecutableElement element = entry.getKey();        // annotation element (method)
            Name elementName = element.getSimpleName();        // e.g. "value" or "number"
            prnt ("elementName: " + elementName.toString() + "\n");
            if ( elementName.toString().equals(keyName) ) {
                AnnotationValue value = entry.getValue();          // the value
                Object v = value.getValue();                       // primitive, String, TypeMirror, List<AnnotationValue>, VariableElement (enum), etc.
                return Optional.ofNullable(v);
            }
        } return Optional.empty ();
    }
    static Optional <Object> getAnnValue (
        ProcessingEnvironment processingEnv,
        AnnotationMirror am,
        String amName,
        String keyName
        ) {
            if (amName.toString().equals (amName)) {
                return getAnnValue (processingEnv, am, keyName);
            } return Optional.empty ();
    }

}