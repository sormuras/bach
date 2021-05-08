package com.github.sormuras.bach.internal;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

public record Records<R extends Record>(Class<R> type) {

  public static <R extends Record> Records<R> of(Class<R> type) {
    return new Records<>(type);
  }

  @SuppressWarnings("unchecked")
  Cache<R> cache() {
    return (Cache<R>) CACHES.get(type);
  }

  public R compose(Function<RecordComponent, Object> function) {
    var cache = cache();
    var values = Stream.of(type.getRecordComponents()).map(function).toArray(Object[]::new);
    return cache.newRecord(values);
  }

  @SafeVarargs
  public final R compose(
      R initial,
      Predicate<RecordComponent> componentFilter,
      Predicate<Object> valueFilter,
      BiConsumer<RecordComponent, Integer> valueConsumer,
      R... layers) {
    if (layers.length == 0) return initial;
    var cache = cache();
    var components = type.getRecordComponents();
    var values = cache.values(initial);
    withNextComponent:
    for (int componentIndex = 0; componentIndex < components.length; componentIndex++) {
      var component = components[componentIndex];
      if (componentFilter.test(component)) {
        for (int layerIndex = 0; layerIndex < layers.length; layerIndex++) {
          var layer = layers[layerIndex];
          var value = value(layer, component);
          if (valueFilter.test(value)) {
            valueConsumer.accept(component, layerIndex);
            values[componentIndex] = value;
            continue withNextComponent;
          }
        }
      }
    }
    return cache.newRecord(values);
  }

  public R with(R instance, String name, Object value) {
    var cache = cache();
    var index = cache.indexOf(name);
    return with(cache, instance, index, value);
  }

  private R with(Cache<R> cache, R instance, int index, Object value) {
    var values = cache.values(instance);
    values[index] = value;
    return cache.newRecord(values);
  }

  public Object value(R instance, RecordComponent component) {
    return cache().value(instance, component);
  }

  private static class Caches extends ClassValue<Cache<?>> {

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    protected Cache<?> computeValue(Class<?> type) {
      return new Cache(type, computeConstructor(type), computeIndex(type));
    }

    // https://bugs.openjdk.java.net/browse/JDK-8265356
    private MethodHandle computeConstructor(Class<?> type) {
      var components = type.getRecordComponents();
      var length = components.length;
      var canonicalTypes = new Class<?>[length];
      for (int i = 0; i < length; i++) canonicalTypes[i] = components[i].getType();
      try {
        Constructor<?> constructor = type.getDeclaredConstructor(canonicalTypes);
        constructor.setAccessible(true);
        return MethodHandles.lookup().unreflectConstructor(constructor);
      } catch (ReflectiveOperationException exception) {
        throw new AssertionError("Canonical constructor computation failed for " + type, exception);
      }
    }

    private Map<String, Integer> computeIndex(Class<?> type) {
      var map = new TreeMap<String, Integer>();
      var index = 0;
      for (var component : type.getRecordComponents()) {
        map.put(component.getName(), index);
        var annotation = component.getAnnotation(Name.class);
        if (annotation != null) for (var alias : annotation.value()) map.put(alias, index);
        index++;
      }
      return map;
    }
  }

  private static final Caches CACHES = new Caches();

  record Cache<R extends Record>(
      Class<R> type, MethodHandle constructor, Map<String, Integer> index) {

    int indexOf(String component) {
      var index = index().getOrDefault(component, -1);
      if (index == -1) throw new NoSuchElementException(component + " not in: " + index().keySet());
      return index;
    }

    Object[] values(R instance) {
      var components = type.getRecordComponents();
      var length = components.length;
      var values = new Object[length];
      for (int i = 0; i < length; i++) values[i] = value(instance, components[i]);
      return values;
    }

    Object value(Object instance, RecordComponent component) {
      try {
        return component.getAccessor().invoke(instance);
      } catch (ReflectiveOperationException exception) {
        var identity = System.identityHashCode(instance);
        throw new AssertionError("access failed for " + component + " on @" + identity, exception);
      }
    }

    @SuppressWarnings("unchecked")
    R newRecord(Object[] initargs) {
      try {
        return (R) constructor.invokeWithArguments(initargs);
      } catch (Throwable exception) {
        throw new AssertionError("new " + type.getSimpleName() + " failed", exception);
      }
    }
  }

  @Target(ElementType.RECORD_COMPONENT)
  @Retention(RetentionPolicy.RUNTIME)
  public @interface Name {
    String[] value();
  }
}
