/*
 * Bach - Java Shell Builder
 * Copyright (C) 2020 Christian Stein
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

import de.sormuras.bach.project.library.AsmModules;
import de.sormuras.bach.project.library.JUnitJupiterModules;
import de.sormuras.bach.project.library.JUnitPlatformModules;
import de.sormuras.bach.project.library.JUnitVintageModules;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.function.Function;

/** A locator maps module names to URIs of modular JAR files. */
public interface Locator extends Function<String, URI> {

  /** Return the {@link URI} for the given module name. */
  @Override
  URI apply(String module);

  static Locator of() {
    return new DefaultLocator();
  }

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

  /** Map-backed locator implementation. */
  abstract class AbstractLocator extends TreeMap<String, String> implements Locator {

    public String put(String module, String gav, long size, String md5) {
      return put(module, Maven.central(gav, module, size, md5));
    }

    @Override
    public URI apply(String module) {
      var uri = get(module);
      if (uri == null) return null;
      return URI.create(uri);
    }

    @Override
    public String toString() {
      return getClass().getSimpleName() + " [" + size() + " modules]";
    }
  }

  class DefaultLocator extends AbstractLocator {
    public DefaultLocator() {
      putAll(new AsmModules());
      putAll(new JUnitPlatformModules());
      putAll(new JUnitJupiterModules());
      putAll(new JUnitVintageModules());
    }
  }

  /** Maven-related URI-string building helpers. */
  interface Maven {

    String CENTRAL_REPOSITORY = "https://repo.maven.apache.org/maven2";

    static String central(String mavenGroupArtifactVersion, String module, long size, String md5) {
      var coordinates = mavenGroupArtifactVersion.split(":");
      var group = coordinates[0];
      var artifact = coordinates[1];
      var version = coordinates[2];
      var classifier = coordinates.length < 4 ? "" : coordinates[3];
      var ssp = maven(CENTRAL_REPOSITORY, group, artifact, version, classifier);
      var attributes = new LinkedHashMap<String, String>();
      attributes.put("module", module);
      attributes.put("version", version);
      if (size >= 0) attributes.put("size", Long.toString(size));
      if (!md5.isEmpty()) attributes.put("md5", md5);
      return ssp + '#' + toFragment(attributes);
    }

    static String central(String group, String artifact, String version) {
      return maven(CENTRAL_REPOSITORY, group, artifact, version, "");
    }

    static String maven(String repository, String g, String a, String v, String classifier) {
      var filename = a + '-' + (classifier.isEmpty() ? v : v + '-' + classifier);
      var joiner = new StringJoiner("/").add(repository);
      joiner.add(g.replace('.', '/')).add(a).add(v).add(filename + ".jar");
      return joiner.toString();
    }
  }
}
