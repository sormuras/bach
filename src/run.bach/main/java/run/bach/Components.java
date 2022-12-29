package run.bach;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class Components {
  private final Map<Class<?>, Object> map;

  public Components() {
    this.map = new ConcurrentHashMap<>();
  }

  @SuppressWarnings("unchecked")
  public <T> T get(Class<T> type) {
    return (T) map.get(type);
  }

  public <T> Components put(Class<T> type, T value) {
    map.put(type, value);
    return this;
  }
}
