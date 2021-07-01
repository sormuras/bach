package com.github.sormuras.bach.internal;

import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Optional;

/** {@link RecordComponent}-related utilities. */
public record RecordComponents(Class<? extends Record> type) {

  public static RecordComponents of(Class<? extends Record> type) {
    return new RecordComponents(type);
  }

  public Optional<RecordComponent> findUnique(Class<?> candidate) {
    var components = new ArrayList<RecordComponent>();
    for (var component : type.getRecordComponents())
      if (component.getType().isAssignableFrom(candidate)) components.add(component);
    if (components.size() == 1) return Optional.of(components.get(0));
    return Optional.empty();
  }
}
