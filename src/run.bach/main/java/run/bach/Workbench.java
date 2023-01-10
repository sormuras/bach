package run.bach;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import run.duke.ToolContext;

public final class Workbench implements ToolContext {
  private final Map<Class<?>, Object> map;

  public Workbench(Record... records) {
    this.map = new ConcurrentHashMap<>();
    for (var record : records) put(record);
  }

  public void put(Record record) {
    map.put(record.getClass(), record);
  }

  public <R extends Record> void put(Class<R> key, Supplier<R> init, UnaryOperator<R> operator) {
    var record = findConstant(key).orElseGet(init);
    put(operator.apply(record));
  }

  @SuppressWarnings("unchecked")
  public <R extends Record> R getConstant(Class<R> key) {
    return (R) map.get(key);
  }
}
