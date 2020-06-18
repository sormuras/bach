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

import de.sormuras.bach.project.Project;
import java.lang.System.Logger.Level;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/** A logbook records log entries and tool calls. */
public class Logbook {

  public static final class Entry {
    private final long thread;
    private final Level level;
    private final String text;

    Entry(long thread, Level level, String text) {
      this.thread = thread;
      this.level = level;
      this.text = text;
    }

    @Override
    public String toString() {
      return String.format("%-7s %6X| %s", level, thread, text);
    }
  }

  public static final class Called {
    private final long thread;
    private final String tool;
    private final String[] args;
    private final String out;
    private final String err;
    private final Duration duration;
    private final int code;

    Called(
        long thread,
        String tool,
        String[] args,
        String normal,
        String errors,
        Duration duration,
        int code) {
      this.thread = thread;
      this.tool = tool;
      this.args = args;
      this.out = normal;
      this.err = errors;
      this.duration = duration;
      this.code = code;
    }

    public boolean error() {
      return code != 0;
    }

    public String toDetailedCaption() {
      return tool + '-' + Integer.toHexString(System.identityHashCode(this));
    }

    public List<String> toStrings() {
      var message = new ArrayList<String>();
      message.add("");
      message.add('\t' + tool + ' ' + String.join(" ", args));
      if (!out.isEmpty()) {
        message.add("");
        out.lines().forEach(line -> message.add("\t\t" + line));
      }
      if (!err.isEmpty()) {
        message.add("");
        err.lines().forEach(line -> message.add("\t\t" + line));
      }
      message.add("");
      return message;
    }
  }

  public static Logbook ofSystem() {
    var logbookThreshold = System.getProperty("logbook.threshold", "INFO");
    return new Logbook(System.out::println, Level.valueOf(logbookThreshold));
  }

  private final Queue<Entry> entries = new ConcurrentLinkedQueue<>();
  private final Queue<Called> calls = new ConcurrentLinkedQueue<>();
  private final Consumer<String> directConsumer;
  private final Level directThreshold;

  public Logbook(Consumer<String> directConsumer, Level directThreshold) {
    this.directConsumer = directConsumer;
    this.directThreshold = directThreshold;
  }

  public Consumer<String> consumer() {
    return directConsumer;
  }

  public Level threshold() {
    return directThreshold;
  }

  public Logbook with(Consumer<String> consumer) {
    return new Logbook(consumer, directThreshold);
  }

  public Logbook with(Level threshold) {
    return new Logbook(directConsumer, threshold);
  }

  public void print(Level level, String format, Object... arguments) {
    print(level, String.format(format, arguments));
  }

  public void print(Level level, String text) {
    if (text.isEmpty()) return;
    var thread = Thread.currentThread().getId();
    var entry = new Entry(thread, level, text);
    entries.add(entry);
    if (level.getSeverity() < directThreshold.getSeverity()) return;
    synchronized (entries) {
      var all = directThreshold == Level.ALL;
      directConsumer.accept(all ? entry.toString() : text);
    }
  }

  public void called(Called call) {
    calls.add(call);
    print(Level.TRACE, call.out);
    print(Level.TRACE, call.err);
  }

  public List<Called> errors() {
    return calls.stream().filter(Called::error).collect(Collectors.toList());
  }

  public List<String> toMarkdown(Project project) {
    var md = new ArrayList<String>();
    md.add("# Logbook of " + project.toNameAndVersion());
    md.addAll(projectDescription(project));
    md.addAll(toolCallOverview());
    md.addAll(toolCallDetails());
    md.addAll(logbookEntries());
    return md;
  }

  private List<String> projectDescription(Project project) {
    var md = new ArrayList<String>();
    md.add("");
    md.add("## Project");
    md.add("- name: " + project.basics().name());
    md.add("- version: " + project.basics().version());
    md.add("");
    md.add("### Project Descriptor");
    md.add("```text");
    md.addAll(project.toStrings());
    md.add("```");
    return md;
  }

  private List<String> toolCallOverview() {
    var md = new ArrayList<String>();
    md.add("");
    md.add("## Tool Call Overview");
    md.add("|    |Thread|Duration|Tool|Arguments");
    md.add("|----|-----:|-------:|----|---------");
    for (var call : calls) {
      var kind = ' ';
      var thread = call.thread;
      var millis = call.duration.toMillis();
      var tool = "[" + call.tool + "](#" + call.toDetailedCaption() + ")";
      var arguments = String.join(" ", call.args);
      var row = String.format("|%4c|%6X|%8d|%s|%s", kind, thread, millis, tool, arguments);
      md.add(row);
    }
    return md;
  }

  private List<String> toolCallDetails() {
    var md = new ArrayList<String>();
    md.add("");
    md.add("## Tool Call Details");
    for (var call : calls) {
      md.add("");
      md.add("### " + call.toDetailedCaption());
      md.add("- tool = `" + call.tool + '`');
      md.add("- args = `" + String.join(" ", call.args) + '`');
      if (!call.out.isEmpty()) {
        md.add("```text");
        md.add(call.out);
        md.add("```");
      }
      if (!call.err.isEmpty()) {
        md.add("```text");
        md.add(call.err);
        md.add("```");
      }
    }
    return md;
  }

  private List<String> logbookEntries() {
    var md = new ArrayList<String>();
    md.add("");
    md.add("## All Entries");
    md.add("```");
    for (var entry : entries) md.add(entry.toString());
    md.add("```");
    return md;
  }
}
