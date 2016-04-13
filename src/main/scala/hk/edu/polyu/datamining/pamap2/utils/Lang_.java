package hk.edu.polyu.datamining.pamap2.utils;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;

/**
 * Created by beenotung on 2/27/16.
 */
public class Lang_ {
  public interface Producer<E> {
    E apply();
  }

  public interface Consumer<E> {
    void apply(E e);
  }

  public interface ProducerConsumer<A, B> {
    B apply(A a);
  }

  public interface Function {
    void apply();
  }

  public static String[] toStringArray(List<String> list) {
    String[] array = new String[list.size()];
    return list.toArray(array);
  }
}
