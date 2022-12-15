package run.duke;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class Workpieces {
  private final Map<Class<?>, Object> map;

  public Workpieces() {
    this.map = new ConcurrentHashMap<>();
  }

  @SuppressWarnings("unchecked")
  public <T> T get(Class<T> type) {
    return (T) map.get(type);
  }

  public <T> Workpieces put(Class<T> type, T value) {
    map.put(type, value);
    return this;
  }
}
