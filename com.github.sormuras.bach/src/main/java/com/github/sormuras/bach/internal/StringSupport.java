package com.github.sormuras.bach.internal;

import java.util.List;
import java.util.stream.Collectors;

public interface StringSupport {
  static String join(List<String> strings) {
    return strings.stream().map(StringSupport::firstLine).collect(Collectors.joining(" "));
  }

  static String firstLine(String string) {
    var lines = string.lines().toList();
    var size = lines.size();
    return size <= 1 ? string : lines.get(0) + "[...%d lines]".formatted(size);
  }

  record Property(String key, String value) {}

  static Property parseProperty(String string) {
    return StringSupport.parseProperty(string, '=');
  }

  static Property parseProperty(String string, char separator) {
    int index = string.indexOf(separator);
    if (index < 0) {
      var message = "Expected a `KEY%sVALUE` string, but got: %s".formatted(separator, string);
      throw new IllegalArgumentException(message);
    }
    var key = string.substring(0, index);
    var value = string.substring(index + 1);
    return new Property(key, value);
  }
}
