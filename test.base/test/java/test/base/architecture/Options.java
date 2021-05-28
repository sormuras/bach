package test.base.architecture;

import java.lang.Character.UnicodeScript;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

record Options(
    Boolean a, List<String> b, Integer c, Double d, List<UnicodeScript> e, List<String> f) {

  static Options of(String... args) {
    var options = new Options(null, null, null, null, null, null);
    for (int i = 0; i < args.length; i++) {
      var current = args[i];
      if (!current.startsWith("--")) {
        options = options.with("--f", current);
        continue;
      }
      options = options.with(current, args[++i]);
    }
    return options;
  }

  Options underlay(Options... layers) {
    if (layers.length == 0) return this;
    return new Options(
        underlay(a, Options::a, layers),
        underlay(b, Options::b, layers),
        underlay(c, Options::c, layers),
        underlay(d, Options::d, layers),
        underlay(e, Options::e, layers),
        underlay(f, Options::f, layers));
  }

  private static <T> T underlay(T initialValue, Function<Options, T> function, Options... layers) {
    if (initialValue != null) return initialValue;
    for (var layer : layers) {
      var value = function.apply(layer);
      if (value != null) return value;
    }
    return null;
  }

  Options with(String option, String text) {
    return with(Map.of(option, text == null ? Optional.empty() : Optional.of(List.of(text))));
  }

  Options with(String option, String text, String... more) {
    return with(
        Map.of(option, Optional.of(Stream.concat(Stream.of(text), Stream.of(more)).toList())));
  }

  Options with(Map<String, Optional<List<String>>> map) {
    return new Options(
        withSetting(a, map.get("--a"), Boolean::parseBoolean),
        withMerging(b, map.get("--b")),
        withSetting(c, map.get("--c"), Integer::parseInt),
        withSetting(d, map.get("--d"), Double::parseDouble),
        withMerging(e, map.get("--e"), UnicodeScript::valueOf),
        withMerging(f, map.get("--f")));
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private static String withSetting(String old, Optional<List<String>> entry) {
    return withSetting(old, entry, Function.identity());
  }

  @SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "OptionalAssignedToNull"})
  private static <T> T withSetting(
      T old, Optional<List<String>> entry, Function<String, T> mapper) {
    if (entry == null) return old;
    if (entry.isEmpty()) return null;
    return entry.map(strings -> strings.get(0)).map(mapper).orElse(old);
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private static List<String> withMerging(List<String> old, Optional<List<String>> entry) {
    return withMerging(old, entry, Function.identity());
  }

  @SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "OptionalAssignedToNull"})
  private static <T> List<T> withMerging(
      List<T> old, Optional<List<String>> entry, Function<String, T> mapper) {
    if (entry == null) return old;
    if (entry.isEmpty()) return null;
    if (old == null) return entry.get().stream().map(mapper).toList();
    return entry
        .map(strings -> Stream.concat(old.stream(), strings.stream().map(mapper)))
        .map(Stream::toList)
        .orElse(old);
  }
}
