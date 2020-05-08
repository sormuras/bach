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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.spi.ToolProvider;

/** Tool call API consisting of a provider and its arguments, as a {@link String} array. */
public interface Tool {

  /** Return the tool provider to run. */
  ToolProvider provider();

  /** Return the arguments to pass to {@code ToolProvider#run(out, err, String...)}. */
  String[] arguments();

  /** Return {@code true} if the given object is not null in any form, otherwise {@code false}. */
  static boolean assigned(Object object) {
    if (object == null) return false;
    if (object instanceof Number) return ((Number) object).intValue() != 0;
    if (object instanceof String) return !((String) object).isEmpty();
    if (object instanceof Optional) return ((Optional<?>) object).isPresent();
    if (object instanceof Collection) return !((Collection<?>) object).isEmpty();
    if (object.getClass().isArray()) return Array.getLength(object) != 0;
    return true;
  }

  /** A mutable argument list builder. */
  class Arguments {
    private final List<String> list = new ArrayList<>();

    public Arguments add(Object argument) {
      list.add(argument.toString());
      return this;
    }

    public Arguments add(String key, Object value) {
      return add(key).add(value);
    }

    public Arguments add(Arguments arguments) {
      list.addAll(arguments.list);
      return this;
    }

    public String[] toStringArray() {
      return list.toArray(String[]::new);
    }
  }

  /** A description of the frame or reason a tool is called. */
  final class Context {
    private final String realm;
    private final String module;

    public Context(String realm, String module) {
      this.realm = realm;
      this.module = module;
    }

    public String realm() {
      return realm;
    }

    public String module() {
      return module;
    }
  }

  /** A call to {@code javac}, the Java compiler. */
  class Javac implements Tool {

    private final Arguments additionalArguments = new Arguments();
    private Set<String> compileModulesCheckingTimestamps = Set.of();

    @Override
    public ToolProvider provider() {
      return ToolProvider.findFirst("javac").orElseThrow();
    }

    @Override
    public String[] arguments() {
      var arguments = new Arguments();
      var module = getCompileModulesCheckingTimestamps();
      if (assigned(module)) arguments.add("--module", String.join(",", module));
      return arguments.add(getAdditionalArguments()).toStringArray();
    }

    public Arguments getAdditionalArguments() {
      return additionalArguments;
    }

    public Set<String> getCompileModulesCheckingTimestamps() {
      return compileModulesCheckingTimestamps;
    }

    public Javac setCompileModulesCheckingTimestamps(Set<String> moduleNames) {
      this.compileModulesCheckingTimestamps = moduleNames;
      return this;
    }
  }

  /**
   * Tune arguments passed to tool calls.
   *
   * <p>Sample implementation:
   *
   * <pre><code>
   * if (tool instanceof Javac javac) javac.getArguments().add("--verbose");
   * </code></pre>
   */
  @FunctionalInterface
  interface Tuner {
    void tune(Tool tool, Context context);
  }
}
