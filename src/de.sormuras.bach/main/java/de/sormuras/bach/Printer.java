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

package de.sormuras.bach;

import de.sormuras.bach.util.Strings;
import java.lang.System.Logger.Level;
import java.util.EnumSet;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/** A level-aware message printer. */
public interface Printer {

  default void print(Level level, String... message) {
    if (!printable(level)) return;
    print(level, Strings.text(message));
  }

  default void print(Level level, Iterable<String> message) {
    if (!printable(level)) return;
    print(level, Strings.text(message));
  }

  boolean printable(Level level);

  void print(Level level, String message);

  static Printer ofSystem() {
    var verbose = Boolean.getBoolean("ebug") || "".equals(System.getProperty("ebug"));
    return ofSystem(verbose ? Level.ALL : Level.INFO);
  }

  static Printer ofSystem(Level threshold) {
    return new Default(Printer::systemPrintLine, threshold);
  }

  static void systemPrintLine(Level level, String message) {
    if (level.getSeverity() <= Level.INFO.getSeverity()) System.out.println(message);
    else System.err.println(message);
  }

  class Default implements Printer {

    private final BiConsumer<Level, String> consumer;
    private final Level threshold;

    public Default(BiConsumer<Level, String> consumer, Level threshold) {
      this.consumer = consumer;
      this.threshold = threshold;
    }

    @Override
    public boolean printable(Level level) {
      if (threshold == Level.OFF) return false;
      return threshold == Level.ALL || threshold.getSeverity() <= level.getSeverity();
    }

    @Override
    public void print(Level level, String message) {
      if (!printable(level)) return;
      synchronized (consumer) {
        consumer.accept(level, message);
      }
    }

    @Override
    public String toString() {
      var levels = EnumSet.range(Level.TRACE, Level.ERROR).stream();
      var map = levels.map(level -> level + ":" + printable(level));
      return "Default[threshold=" + threshold + "] -> " + map.collect(Collectors.joining(" "));
    }
  }
}
