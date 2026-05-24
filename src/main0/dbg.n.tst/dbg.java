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
} 