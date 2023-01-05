package run.duke;

import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

public final class Workbench {
  @FunctionalInterface
  public interface ValueFactory {
    <R extends Record> R createValue(Workbench workbench);
  }

  private static final Workbench EMPTY = new Workbench();

  public static Workbench empty() {
    return EMPTY;
  }

  public static Workbench of(ModuleLayer layer) {
    return new Workbench().initialize(layer);
  }

  private final ConcurrentHashMap<Class<?>, Object> map;

  private Workbench() {
    this.map = new ConcurrentHashMap<>();
  }

  private Workbench initialize(ModuleLayer layer) {
    for (var factory : ServiceLoader.load(layer, ValueFactory.class)) {
      var value = factory.createValue(this);
      map.put(value.getClass(), value);
    }
    return this;
  }

  @SuppressWarnings("unchecked")
  public <R extends Record> R get(Class<R> key) {
    return (R) map.get(key);
  }
}
