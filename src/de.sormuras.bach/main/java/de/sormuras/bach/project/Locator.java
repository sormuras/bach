package de.sormuras.bach.project;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Function;

/** A locator maps module names to URIs of modular JAR files. */
public interface Locator extends Function<String, URI> {

  /** Return the {@link URI} for the given module name. */
  @Override
  URI apply(String module);

  static Map<String, String> parseFragment(String fragment) {
    if (fragment.isEmpty()) return Map.of();
    if (fragment.length() < "a=b".length())
      throw new IllegalArgumentException("Fragment too short: " + fragment);
    if (fragment.indexOf('=') == -1)
      throw new IllegalArgumentException("At least one = expected: " + fragment);
    var map = new LinkedHashMap<String, String>();
    var parts = fragment.split("[&=]");
    for (int i = 0; i < parts.length; i += 2) map.put(parts[i], parts[i + 1]);
    return map;
  }

  static String toFragment(Map<String, String> map) {
    var joiner = new StringJoiner("&");
    map.forEach((key, value) -> joiner.add(key + '=' + value));
    return joiner.toString();
  }
}
