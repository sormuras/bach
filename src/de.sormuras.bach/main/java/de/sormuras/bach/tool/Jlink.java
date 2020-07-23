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
import java.util.List;
import java.util.function.UnaryOperator;

/** A {@code jlink} call configuration. */
public final class Jlink implements Call<Jlink> {

  @FunctionalInterface
  public interface Tweak extends UnaryOperator<Jlink> {}

  private final List<Argument> arguments;

  public Jlink(List<Argument> arguments) {
    this.arguments = arguments;
  }

  @Override
  public String name() {
    return "jlink";
  }

  @Override
  public List<Argument> arguments() {
    return arguments;
  }

  @Override
  public String toDescriptiveLine() {
    var value = findValue("--add-modules");
    if (value.isEmpty()) return Call.super.toDescriptiveLine();
    var modules = value.get().split(",");
    var caption = "Assemble custom runtime image for ";
    if (modules.length == 1) return caption + "module " + modules[0];
    return String.format(caption + "%d modules: %s", modules.length, String.join(", ", modules));
  }

  @Override
  public Jlink with(List<Argument> arguments) {
    return new Jlink(arguments);
  }
}
