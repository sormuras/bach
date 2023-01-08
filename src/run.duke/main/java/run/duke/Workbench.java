package run.duke;

import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

public final class Workbench {
  private final Map<Class<?>, Object> map;

  @SafeVarargs
  public <R extends Record> Workbench(R... workpieces) {
    this.map = new ConcurrentHashMap<>();
    for (var workpiece : workpieces) put(workpiece);
  }

  public void putAll(ModuleLayer layer) {
    for (var factory : ServiceLoader.load(layer, WorkpieceFactory.class)) {
      var workpiece = factory.createWorkpiece(this);
      var previous = put(workpiece);
      if (previous == null) continue;
      throw new AssertionError("Workpiece already set: " + workpiece);
    }
  }

  @SuppressWarnings("unchecked")
  public <R extends Record> R put(R workpiece) {
    var key = workpiece.getClass();
    return (R) map.put(key, workpiece);
  }

  @SuppressWarnings("unchecked")
  public <R extends Record> R get(Class<R> key) {
    return (R) map.get(key);
  }
}
