package com.github.sormuras.bach.internal;

public sealed interface StringSupport permits ConstantInterface {

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
