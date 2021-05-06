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
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

public record Records<R extends Record>(Class<R> type) {

  @Target(ElementType.RECORD_COMPONENT)
  @Retention(RetentionPolicy.RUNTIME)
  public @interface Name {
    String[] value();
  }

  public R compose(Function<RecordComponent, Object> function) {
    @SuppressWarnings("unchecked")
    var cache = (Cache<R>) CACHES.get(type);
    var values = Stream.of(type.getRecordComponents()).map(function).toArray(Object[]::new);
    return cache.newRecord(values);
  }

  public record Composition(int index, RecordComponent component, Object newValue) {}

  @SafeVarargs
  public final R compose(
      R initial,
      Predicate<RecordComponent> componentFilter,
      Predicate<Object> valueFilter,
      Consumer<Composition> consumer,
      R... layers) {
    if (layers.length == 0) return initial;
    @SuppressWarnings("unchecked")
    var cache = (Cache<R>) CACHES.get(type);
    var components = type.getRecordComponents();
    var values = cache.values(initial);
    withNextComponent:
    for (int componentIndex = 0; componentIndex < components.length; componentIndex++) {
      var component = components[componentIndex];
      if (componentFilter.test(component)) {
        for (int layerIndex = 0; layerIndex < layers.length; layerIndex++) {
          var layer = layers[layerIndex];
          var value = getValue(layer, component);
          if (valueFilter.test(value)) {
            consumer.accept(new Composition(layerIndex, component, value));
            values[componentIndex] = value;
            continue withNextComponent;
          }
        }
      }
    }
    return cache.newRecord(values);
  }

  public R with(R instance, String name, Object value) {
    return with(instance, name, __ -> value, (o, n) -> n);
  }

  public R with(
      R instance,
      String name,
      Function<RecordComponent, Object> wrapper,
      BinaryOperator<Object> merger) {
    @SuppressWarnings("unchecked")
    var cache = (Cache<R>) CACHES.get(type);
    var index = cache.indexOf(name);
    return with(cache, instance, index, wrapper, merger);
  }

  private R with(
      Cache<R> cache,
      R instance,
      int index,
      Function<RecordComponent, Object> wrapper,
      BinaryOperator<Object> merger) {
    var values = cache.values(instance);
    var oldValue = values[index];
    var newValue = wrapper.apply(type.getRecordComponents()[index]);
    values[index] = merger.apply(oldValue, newValue);
    return cache.newRecord(values);
  }

  private static Object getValue(Object instance, RecordComponent component) {
    try {
      return component.getAccessor().invoke(instance);
    } catch (ReflectiveOperationException exception) {
      var identity = System.identityHashCode(instance);
      throw new AssertionError("access failed for " + component + " on @" + identity, exception);
    }
  }

  private record Cache<R extends Record>(
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
      for (int i = 0; i < length; i++) values[i] = getValue(instance, components[i]);
      return values;
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
}
