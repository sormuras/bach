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

package de.sormuras.bach.execution;

import de.sormuras.bach.Bach;
import de.sormuras.bach.Summary;
import java.io.StringWriter;
import java.lang.System.Logger.Level;
import java.time.Duration;
import java.time.Instant;

/** Task execution context passed {@link Task#execute(ExecutionContext)}. */
public /*static*/ final class ExecutionContext {

  private final Bach bach;
  private final Summary summary;
  private final Instant start;
  private final StringWriter out;
  private final StringWriter err;

  public ExecutionContext(Bach bach, Summary summary) {
    this.bach = bach;
    this.summary = summary;
    this.start = Instant.now();
    this.out = new StringWriter();
    this.err = new StringWriter();
  }

  public Bach bach() {
    return bach;
  }

  public Summary summary() {
    return summary;
  }

  public Instant start() {
    return start;
  }

  /** Print message if verbose flag is set. */
  public void print(Level level, String format, Object... args) {
    if (bach.debug() || level.getSeverity() >= Level.INFO.getSeverity()) {
      var writer = level.getSeverity() <= Level.INFO.getSeverity() ? out : err;
      writer.write(String.format(format, args));
      writer.write(System.lineSeparator());
    }
  }

  /** Create result with code zero and empty output strings. */
  public ExecutionResult ok() {
    var duration = Duration.between(start(), Instant.now());
    return new ExecutionResult(0, duration, out.toString(), err.toString(), null);
  }

  /** Create result with error code one and append throwable's message to the error string. */
  public ExecutionResult failed(Throwable throwable) {
    var duration = Duration.between(start(), Instant.now());
    return new ExecutionResult(1, duration, out.toString(), err.toString(), throwable);
  }
}
