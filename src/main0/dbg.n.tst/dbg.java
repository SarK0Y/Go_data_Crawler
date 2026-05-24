package dbg.n.tst;
import java.lang.reflect.Method;
import java.lang.Class;
import java.util.ArrayList;
import java.util.List;
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

 */