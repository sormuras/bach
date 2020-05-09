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

import de.sormuras.bach.Tool;
import java.io.File;
import java.lang.reflect.Array;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;

/** An abstract tool implementation providing support for additional arguments. */
public /*static*/ abstract class AbstractTool implements Tool {

  /** Return {@code true} if the given object is not null in any form, otherwise {@code false}. */
  public static boolean assigned(Object object) {
    if (object == null) return false;
    if (object instanceof Number) return ((Number) object).intValue() != 0;
    if (object instanceof String) return !((String) object).isEmpty();
    if (object instanceof Optional) return ((Optional<?>) object).isPresent();
    if (object instanceof Collection) return !((Collection<?>) object).isEmpty();
    if (object.getClass().isArray()) return Array.getLength(object) != 0;
    return true;
  }

  public static String join(Collection<Path> paths) {
    return paths.stream().map(Path::toString).collect(Collectors.joining(File.pathSeparator));
  }

  public static String joinPaths(Collection<String> paths) {
    return String.join(File.pathSeparator, paths);
  }

  private final String name;
  private final Arguments additionalArguments = new Arguments();

  public AbstractTool(String name) {
    this.name = name;
  }

  public Arguments getAdditionalArguments() {
    return additionalArguments;
  }

  @Override
  public ToolProvider toolProvider() {
    return ToolProvider.findFirst(name).orElseThrow();
  }

  @Override
  public String[] toolArguments() {
    var arguments = new Arguments();
    arguments(arguments);
    return arguments.add(getAdditionalArguments()).toStringArray();
  }

  protected void arguments(Arguments arguments) {}
}
