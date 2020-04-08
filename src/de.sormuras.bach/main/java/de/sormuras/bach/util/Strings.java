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

package de.sormuras.bach.util;

import java.io.File;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** {@link String}-related utilities. */
public /*static*/ class Strings {

  public static List<String> list(String tool, String... args) {
    return list(tool, List.of(args));
  }

  public static List<String> list(String tool, List<String> args) {
    if (args.isEmpty()) return List.of(tool);
    if (args.size() == 1) return List.of(tool + ' ' + args.get(0));
    var strings = new ArrayList<String>();
    strings.add(tool + " with " + args.size() + " arguments:");
    var simple = true;
    for (String arg : args) {
      var minus = arg.startsWith("-");
      strings.add((simple | minus ? "\t" : "\t\t") + arg);
      simple = !minus;
    }
    return List.copyOf(strings);
  }

  public static String text(String... lines) {
    return String.join(System.lineSeparator(), lines);
  }

  public static String text(Iterable<String> lines) {
    return String.join(System.lineSeparator(), lines);
  }

  public static String text(Stream<String> lines) {
    return String.join(System.lineSeparator(), lines.collect(Collectors.toList()));
  }

  public static String textIndent(String indent, String... strings) {
    return indent + String.join(System.lineSeparator() + indent, strings);
  }

  public static String textIndent(String indent, Iterable<String> strings) {
    return indent + String.join(System.lineSeparator() + indent, strings);
  }

  public static String textIndent(String indent, Stream<String> strings) {
    return indent + text(strings.map(string -> indent + string));
  }

  public static String toString(Duration duration) {
    return duration
        .truncatedTo(TimeUnit.MILLISECONDS.toChronoUnit())
        .toString()
        .substring(2)
        .replaceAll("(\\d[HMS])(?!$)", "$1 ")
        .toLowerCase();
  }

  public static String toString(Collection<Path> paths) {
    return paths.stream().map(Path::toString).collect(Collectors.joining(File.pathSeparator));
  }

  private Strings() {}
}
