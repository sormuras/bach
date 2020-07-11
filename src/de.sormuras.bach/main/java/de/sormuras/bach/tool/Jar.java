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

package de.sormuras.bach.tool;

import de.sormuras.bach.Call;
import java.nio.file.Path;
import java.util.List;

/** A {@code jar} call configuration. */
public final class Jar implements Call<Jar> {

  private final List<Argument> arguments;

  public Jar(List<Argument> arguments) {
    this.arguments = List.copyOf(arguments);
  }

  @Override
  public String name() {
    return "jar";
  }

  @Override
  public List<Argument> arguments() {
    return arguments;
  }

  @Override
  public String toDescriptiveLine() {
    var value = findValue("--file");
    if (value.isEmpty()) return Call.super.toDescriptiveLine();
    return "Create archive " + Path.of(value.get()).getFileName();
  }

  @Override
  public Jar with(List<Argument> arguments) {
    if (arguments == arguments()) return this;
    return new Jar(arguments);
  }

  public Jar withArchiveFile(Path path) {
    return with("--file", path);
  }
}
