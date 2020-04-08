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

package test.base;

import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/** Collecting logger and message line consumer implementation. */
public class Log implements BiConsumer<Level, String> {

  private final Collection<Entry> entries = new ConcurrentLinkedQueue<>();

  @Override
  public void accept(Level level, String message) {
    entries.add(new Entry(level, message));
  }

  public void assertThatEverythingIsFine() {
    var issues =
        entries.stream()
            .filter(entry -> entry.level.getSeverity() >= Level.WARNING.getSeverity())
            .collect(Collectors.toList());
    if (issues.isEmpty()) return;
    var messages = new StringJoiner(System.lineSeparator());
    for (var issue : issues) messages.add(issue.message);
    throw new AssertionError("Not ok!\n" + messages);
  }

  public List<String> lines() {
    var lines = new ArrayList<String>();
    for (var entry : entries) lines.addAll(entry.message.lines().collect(Collectors.toList()));
    return lines;
  }

  public static final class Entry {
    private final Level level;
    private final String message;

    public Entry(Level level, String message) {
      this.level = level;
      this.message = message;
    }

    @Override
    public String toString() {
      return new StringJoiner(", ", "Log.Entry[", "]")
          .add("level=" + level)
          .add("message='" + message + "'")
          .toString();
    }
  }
}
