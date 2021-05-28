package test.base.architecture;

import java.lang.Character.UnicodeScript;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

record Options(
    Boolean a, List<String> b, Integer c, Double d, List<UnicodeScript> e, List<String> f, String g) {

  static Options of(String... args) {
    var options = new Options(null, null, null, null, null, null, null);
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
        underlay(f, Options::f, layers),
        underlay(g, Options::g, layers)
    );
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
    return with(Map.of(option, new Value(text)));
  }

  Options with(String option, String text, String... more) {
    return with(Map.of(option, new Value(text, more)));
  }

  private record Value(String text, String... more) {}

  private Options with(Map<String, Value> map) {
    return new Options(
        withSetting(a, map.get("--a"), Boolean::parseBoolean),
        withMerging(b, map.get("--b")),
        withSetting(c, map.get("--c"), Integer::parseInt),
        withSetting(d, map.get("--d"), Double::parseDouble),
        withMerging(e, map.get("--e"), UnicodeScript::valueOf),
        withMerging(f, map.get("--f")),
        withSetting(g, map.get("--g")));
  }

  private static String withSetting(String old, Value value) {
    return withSetting(old, value, Function.identity());
  }

  private static <T> T withSetting(T old, Value value, Function<String, T> mapper) {
    if (value == null) return old;
    if (value.text == null) return null;
    return mapper.apply(value.text);
  }

  private static List<String> withMerging(List<String> old, Value value) {
    return withMerging(old, value, Function.identity());
  }

  private static <T> List<T> withMerging(List<T> old, Value value, Function<String, T> mapper) {
    if (value == null) return old;
    if (value.text == null) return null;
    var list = new ArrayList<T>();
    if (old != null) list.addAll(old);
    list.add(mapper.apply(value.text));
    Stream.of(value.more).map(mapper).forEach(list::add);
    return List.copyOf(list);
  }
}
