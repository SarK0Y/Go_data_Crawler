package cmdLine;
import io.vavr.control.Either;
import java.util.List;
import java.util.ArrayList;
import basix_funx.tuple;
public interface cmd_line <T extends cmd_line <T>>{
    
    static <T> Either <T, Integer> create (ModeOfTask setMode, TypeOfTask setType, String [] args) {
        return Either.right (0);
    };
    public void exec ();
    public void do_last ();
    public void do_all ();
    public void do_1st ();
    public List <tuple <String, String> > AllOneKeyOneVal (String... key);

}
/*
import java.util.Iterator;
import java.util.NoSuchElementException;

public class ArrayIterator<T> implements Iterator<T> {
    private final T[] array;
    private int cursor = 0;          // index of next element to return
    private int lastReturned = -1;   // index of last returned element, -1 if none

    public ArrayIterator(T[] array) {
        this.array = (array == null) ? (T[]) new Object[0] : array;
    }

    @Override
    public boolean hasNext() {
        return cursor < array.length;
    }

    @Override
    public T next() {
        if (!hasNext()) throw new NoSuchElementException();
        lastReturned = cursor;
        return array[cursor++];
    }

    @Override
    public void remove() {
        if (lastReturned == -1) throw new IllegalStateException("next() has not been called or remove() already called");
        int moveCount = array.length - lastReturned - 1;
        if (moveCount > 0) {
            System.arraycopy(array, lastReturned + 1, array, lastReturned, moveCount);
        }
        array[array.length - 1] = null; // clear last slot
        cursor = lastReturned;          // adjust cursor to account for removed element
        lastReturned = -1;
    }
}

 */