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

// default package

import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** Parse https://jdk.java.net pages and print available builds. */
public class ShowJdkBuilds {

  public static void main(String... args) {
    var builds = new TreeMap<>(parseArchive());
    builds.put(17, parse(17)); // GA
    builds.put(18, parse(18)); // EA
    builds.put(19, parse(19)); // EA

    var features = new ArrayList<>(builds.keySet());
    features.sort(Comparator.reverseOrder());
    for (var feature : features) {
      System.out.println();
      System.out.println("#");
      System.out.println("# JDK " + feature);
      System.out.println("#");
      builds.get(feature).forEach((key, value) -> System.out.format("%s=%s%n", key, value));
    }
  }

  /** Parse {@code https://jdk.java.net/${feature}} page. */
  static Map<String, String> parse(int feature) {
    var html = read("https://jdk.java.net/" + feature);
    var table = substring(html, "<table class=\"builds\"", "</table>");
    var map = new TreeMap<String, String>();
    for (var line : lines(table)) {
      var name = Path.of(URI.create(line).getPath()).getFileName().toString();
      var key = feature + "-" + substring(name, "_", "_bin");
      map.put(key, line);
    }
    return map;
  }

  /** Parse {@code https://jdk.java.net/archive} page. */
  static Map<Integer, Map<String, String>> parseArchive() {
    var html = read("https://jdk.java.net/archive");
    var table = substring(html, "<table class=\"builds\"", "</table>");
    var builds = new TreeMap<String, String>();
    for (var line : lines(table)) {
      var name = Path.of(URI.create(line).getPath()).getFileName().toString();
      var version = Runtime.Version.parse(substring(name, "-", "_"));
      var key = version.feature() + "-" + substring(name, "_", "_bin");
      builds.putIfAbsent(key, line);
    }
    var archive = new TreeMap<Integer, Map<String, String>>();
    for (var build : builds.entrySet()) {
      var key = build.getKey();
      var feature = Runtime.Version.parse(key).feature();
      archive.merge(
          feature,
          new TreeMap<>(Map.of(key, build.getValue())),
          (old, map) -> {
            old.putAll(map);
            return old;
          });
    }
    return archive;
  }

  static String read(String url) {
    try (var scanner = new Scanner(new URL(url).openStream(), StandardCharsets.UTF_8)) {
      scanner.useDelimiter("\\A");
      return scanner.hasNext() ? scanner.next() : "";
    } catch (Exception e) {
      throw new Error("Read failed for: " + url, e);
    }
  }

  static String substring(String string, String beginTag, String endTag) {
    int beginIndex = string.indexOf(beginTag);
    if (beginIndex < 0) {
      System.err.println("beginTag not found: " + beginTag);
      System.err.println("string: " + string);
      return "";
    }
    int endIndex = string.indexOf(endTag, beginIndex + beginTag.length());
    if (endIndex < 0) {
      System.err.println("endTag not found: " + endTag);
      System.err.println("string: " + string);
      return "";
    }
    return string.substring(beginIndex + beginTag.length(), endIndex).trim();
  }

  static List<String> lines(String table) {
    var tags = table.lines().filter(line -> line.startsWith("<td><a href=\""));
    return tags.map(line -> line.substring(13, line.length() - 2)).collect(Collectors.toList());
  }
}
