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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.spi.ToolProvider;

/** A tool call configuration. */
public interface Call<T> {

  String name();

  List<Argument> arguments();

  T with(List<Argument> arguments);

  default boolean activated() {
    return !arguments().isEmpty();
  }

  default Optional<ToolProvider> tool() {
    return ToolProvider.findFirst(name());
  }

  default T with(String option) {
    var list = new ArrayList<>(arguments());
    list.add(new Argument(option, List.of()));
    return with(list);
  }

  default T with(String option, Object... values) {
    var strings = new ArrayList<String>();
    for (var value : values) strings.add(value.toString());
    var list = new ArrayList<>(arguments());
    list.add(new Argument(option, strings));
    return with(list);
  }

  default String[] toStringArray() {
    var list = new ArrayList<String>();
    for (var argument : arguments()) {
      list.add(argument.key());
      list.addAll(argument.values());
    }
    return list.toArray(String[]::new);
  }
}
