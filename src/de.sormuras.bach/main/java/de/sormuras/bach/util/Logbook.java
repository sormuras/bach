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

import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** An entry-collecting system logger implementation. */
public /*static*/ class Logbook implements System.Logger {

  public static Logbook ofSystem() {
    var debug = Boolean.getBoolean("ebug") || "".equals(System.getProperty("ebug"));
    var dryRun = Boolean.getBoolean("ry-run") || "".equals(System.getProperty("ry-run"));
    return new Logbook(System.out::println, debug, dryRun);
  }

  private final Consumer<String> consumer;
  private final boolean debug;
  private final boolean dryRun;
  private final Collection<Entry> entries;

  public Logbook(Consumer<String> consumer, boolean debug, boolean dryRun) {
    this.consumer = consumer;
    this.debug = debug;
    this.dryRun = dryRun;
    this.entries = new ConcurrentLinkedQueue<>();
  }

  public boolean isDebug() {
    return debug;
  }

  public boolean isDryRun() {
    return dryRun;
  }

  @Override
  public String getName() {
    return "Logbook";
  }

  @Override
  public boolean isLoggable(Level level) {
    if (level == Level.ALL) return isDebug();
    if (level == Level.OFF) return isDryRun();
    return true;
  }

  @Override
  public void log(Level level, ResourceBundle bundle, String message, Throwable thrown) {
    if (message == null || message.isBlank()) return;
    var entry = new Entry(level, message, thrown);
    if (debug) consumer.accept(entry.toString());
    entries.add(entry);
  }

  @Override
  public void log(Level level, ResourceBundle bundle, String pattern, Object... arguments) {
    var message = arguments == null ? pattern : MessageFormat.format(pattern, arguments);
    log(level, bundle, message, (Throwable) null);
  }

  public Stream<Entry> entries(Level threshold) {
    return entries.stream().filter(entry -> entry.level.getSeverity() >= threshold.getSeverity());
  }

  public List<String> messages() {
    return lines(entry -> entry.message);
  }

  public List<String> lines(Function<Entry, String> mapper) {
    return entries.stream().map(mapper).collect(Collectors.toList());
  }

  public static final class Entry {
    private final Level level;
    private final String message;
    private final Throwable exception;

    public Entry(Level level, String message, Throwable exception) {
      this.level = level;
      this.message = message;
      this.exception = exception;
    }

    public Level level() {
      return level;
    }

    public String message() {
      return message;
    }

    public Throwable exception() {
      return exception;
    }

    @Override
    public String toString() {
      var exceptionMessage = exception == null ? "" : " -> " + exception;
      return String.format("%c|%s%s", level.name().charAt(0), message, exceptionMessage);
    }
  }
}
