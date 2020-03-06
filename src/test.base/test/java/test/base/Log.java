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

import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;
import java.util.ResourceBundle;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/** Collecting logger and message line consumer implementation. */
public class Log implements System.Logger, Consumer<String> {

  private final Collection<Entry> entries = new ConcurrentLinkedQueue<>();

  @Override
  public void accept(String message) {
    entries.add(new Entry("P", Level.ALL, message, null));
  }

  @Override
  public String getName() {
    return "Log";
  }

  @Override
  public boolean isLoggable(Level level) {
    return true;
  }

  @Override
  public void log(Level level, ResourceBundle bundle, String msg, Throwable thrown) {
    entries.add(new Entry("L", level, msg, thrown));
  }

  @Override
  public void log(Level level, ResourceBundle bundle, String format, Object... params) {
    entries.add(new Entry("L", level, MessageFormat.format(format, params), null));
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
    return entries.stream().map(e -> e.source + " " + e.message).collect(Collectors.toList());
  }

  public static final class Entry {
    private final String source;
    private final Level level;
    private final String message;
    private final Throwable thrown;

    public Entry(String source, Level level, String message, Throwable thrown) {
      this.source = source;
      this.level = level;
      this.message = message;
      this.thrown = thrown;
    }
  }
}
