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

import java.lang.System.Logger.Level;
import java.net.http.HttpClient;
import java.nio.file.Path;
import test.base.Log;

public class Run {

  private final Bach bach;
  private final Log log;

  public Run() {
    this(Path.of(""));
  }

  public Run(Path base) {
    this(base, new Log(), Level.ALL);
  }

  public Run(Path base, Log log, Level threshold) {
    var printer = new Printer.Default(log, threshold);
    var workspace = Workspace.of(base);
    this.bach = new Bach(printer, workspace, HttpClient.newBuilder()::build);
    this.log = log;
  }

  public Bach bach() {
    return bach;
  }

  public Log log() {
    return log;
  }
}
