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

import java.io.File;
import java.lang.reflect.Array;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;

/** A tool call configuration builder. */
public /*static*/ class Call {

  private final String name;
  private final Arguments additionalArguments = new Arguments();

  public Call(String name) {
    this.name = name;
  }

  public Arguments getAdditionalArguments() {
    return additionalArguments;
  }

  public String toLabel() {
    return name;
  }

  /** Return the tool provider running this tool. */
  public ToolProvider toProvider() {
    return ToolProvider.findFirst(name).orElseThrow();
  }

  /** Return the arguments to pass to {@code ToolProvider#run(out, err, String...)}. */
  public String[] toArguments() {
    var arguments = new Arguments();
    addConfiguredArguments(arguments);
    arguments.list.addAll(additionalArguments.list);
    return arguments.toStringArray();
  }

  protected void addConfiguredArguments(Arguments arguments) {}

  public Task toTask() {
    return new Task.RunTool(toLabel(), toProvider(), toArguments());
  }

  /** A mutable argument list builder. */
  public static class Arguments {
    private final List<String> list = new ArrayList<>();

    public Arguments add(Object argument) {
      list.add(argument.toString());
      return this;
    }

    public Arguments add(String key, Object value) {
      return add(key).add(value);
    }

    public Arguments add(String key, Object first, Object second) {
      return add(key).add(first).add(second);
    }

    public String[] toStringArray() {
      return list.toArray(String[]::new);
    }
  }

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
}
