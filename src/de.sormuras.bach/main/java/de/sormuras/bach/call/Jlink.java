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

package de.sormuras.bach.call;

import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;

/** A call to {@code jlink}, the tool that generates custom runtime images. */
public /*static*/ class Jlink extends AbstractCallBuilder {

  /** Value of {@code --output} option. */
  private Path locationOfTheGeneratedRuntimeImage;
  /** Value of {@code --add-modules <module>(,<module>)*} option. */
  private Set<String> modules;

  public Jlink() {
    super("jlink");
  }

  @Override
  public String toLabel() {
    return "Create a custom runtime image with dependencies for " + getModules();
  }

  @Override
  protected void arguments(Arguments arguments) {
    var output = getLocationOfTheGeneratedRuntimeImage();
    if (assigned(output)) arguments.add("--output", output);
    var modules = getModules();
    if (assigned(modules)) arguments.add("--add-modules", String.join(",", new TreeSet<>(modules)));
  }

  public Path getLocationOfTheGeneratedRuntimeImage() {
    return locationOfTheGeneratedRuntimeImage;
  }

  public Jlink setLocationOfTheGeneratedRuntimeImage(Path output) {
    this.locationOfTheGeneratedRuntimeImage = output;
    return this;
  }

  public Set<String> getModules() {
    return modules;
  }

  public Jlink setModules(Set<String> modules) {
    this.modules = modules;
    return this;
  }
}
