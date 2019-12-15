/*
 * Bach - Java Shell Builder
 * Copyright (C) 2019 Christian Stein
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.sormuras.bach.project;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Project properties enumeration. */
public enum Property {
  NAME("project"),
  VERSION("0"),

  LIBRARY_MODIFIERS(String.join(",", Library.ALL_MODIFIER_NAMES)),
  LIBRARY_REQUIRES(""),

  REALM_MAIN_JAVAC_ARGS("-encoding|UTF-8|-parameters|-Werror|-Xlint"),
  REALM_MAIN_JAVADOC_ARGS("-encoding|UTF-8|-locale|en|-Xdoclint:-missing"),

  REALM_TEST_JAVAC_ARGS("-encoding|UTF-8|-parameters|-Werror|-Xlint:-preview"),

  JAVADOC_MODULES(Realm.ALL_MODULES),

  DEPLOYMENT_POM_TEMPLATE("src/maven-pom-template.xml"),
  DEPLOYMENT_REPOSITORY_ID(null),
  DEPLOYMENT_URL(null);

  private final String key;
  private final String defaultValue;

  Property(String defaultValue) {
    this.key = name().toLowerCase().replace('_', '.');
    this.defaultValue = defaultValue;
  }

  public String getKey() {
    return key;
  }

  public String getDefaultValue() {
    return defaultValue;
  }

  public String get(Properties properties) {
    return get(properties, defaultValue);
  }

  public String get(Properties properties, String defaultValue) {
    return properties.getProperty(key, defaultValue);
  }

  public List<String> list(Properties properties, String regex) {
    return list(properties, regex, Function.identity());
  }

  public <V> List<V> list(Properties properties, String regex, Function<String, V> mapper) {
    var value = get(properties);
    if (value.isBlank()) return List.of();
    var split = value.split(regex);
    return Arrays.stream(split).map(String::strip).map(mapper).collect(Collectors.toList());
  }
}
