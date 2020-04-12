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

import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** A tool call configuration. */
public /*static*/ class Tool {

  public static Tool of(String name, String... arguments) {
    return new Tool(name, List.of(new ObjectArrayOption<>(arguments)));
  }

  public static JavaArchiveTool jar(List<? extends Option> options) {
    return new JavaArchiveTool(options);
  }

  public static JavaCompiler javac(List<? extends Option> options) {
    return new JavaCompiler(options);
  }

  public static JavaDocumentationGenerator javadoc(List<? extends Option> options) {
    return new JavaDocumentationGenerator(options);
  }

  private final String name;
  private final List<? extends Option> options;

  public Tool(String name, List<? extends Option> options) {
    this.name = name;
    this.options = options;

    var type = getClass();
    if (type == Tool.class) return;

    var optionsDeclaredInDifferentClass =
        options.stream()
            .filter(option -> !type.equals(option.getClass().getDeclaringClass()))
            .collect(Collectors.toList());
    if (optionsDeclaredInDifferentClass.isEmpty()) return;

    var caption = String.format("All options of tool %s must be declared in %s", name, type);
    var message = new StringJoiner(System.lineSeparator() + "\tbut ").add(caption);
    for (var option : optionsDeclaredInDifferentClass) {
      var optionClass = option.getClass();
      message.add(optionClass + " is declared in " + optionClass.getDeclaringClass());
    }
    throw new IllegalArgumentException(message.toString());
  }

  public String name() {
    return name;
  }

  public <O extends Option> Stream<O> filter(Class<O> type) {
    return options.stream().filter(option -> option.getClass().equals(type)).map(type::cast);
  }

  public <O extends Option> Optional<O> find(Class<O> type) {
    return filter(type).findAny();
  }

  public <O extends Option> O get(Class<O> type) {
    return find(type).orElseThrow();
  }

  protected void addInitialArguments(Arguments arguments) {}

  protected void addMoreArguments(Arguments arguments) {}

  public List<String> toArgumentStrings() {
    var arguments = new Arguments();
    addInitialArguments(arguments);
    options.forEach(option -> option.visit(arguments));
    addMoreArguments(arguments);
    return arguments.build();
  }
}
