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

package test.base.sandbox;

import java.lang.module.ModuleDescriptor;
import java.lang.reflect.Array;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringJoiner;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Function;

public interface Printable {

  default List<String> print() {
    var lines = new ArrayList<String>();
    print(lines::add, 0, Printable::printValue);
    return lines;
  }

  default void print(Consumer<String> printer, int depth, Function<Object, String> presenter) {
    var indentation = "\t".repeat(depth);
    Class<?> type = getClass();
    printer.accept(indentation + type.getSimpleName() + " {");
    for (var field : type.getDeclaredFields()) {
      var name = field.getName();
      var key = indentation + '\t' + field.getType().getSimpleName() + ' ' + name;
      try {
        var value = field.get(this);
        if (value instanceof Collection) {
          var collection = (Collection<?>) value;
          if (!collection.isEmpty()) {
            if (collection.iterator().next() instanceof Printable) {
              printer.accept(key + " with " + collection.size() + " element(s) {");
              collection.forEach(p -> ((Printable) p).print(printer, depth + 2, presenter));
              printer.accept(indentation + '\t' + '}');
              continue;
            }
          }
        }
        printer.accept(key + '=' + presenter.apply(value));
      } catch (ReflectiveOperationException e) {
        printer.accept(key + " failed: " + e.getMessage());
      }
    }
    printer.accept(indentation + "}");
  }

  static String printValue(Object value) {
    if (value == null) return "null";
    if (value.getClass().isArray()) {
      var length = Array.getLength(value);
      var joiner = new StringJoiner(", ", "[", "]");
      for (int i = 0; i < length; i++) joiner.add(printValue(Array.get(value, i)));
      return joiner.toString();
    }
    if (value instanceof String) return "\"" + value + "\"";
    if (value instanceof Path) {
      var string = String.valueOf(value);
      if (!string.isEmpty()) return string;
      return "'" + value + "' -> " + ((Path) value).toUri();
    }
    if (value instanceof ModuleDescriptor) {
      var module = (ModuleDescriptor) value;
      var joiner = new StringJoiner(", ", "module {", "}");
      joiner.add("name=" + module.toNameAndVersion());
      joiner.add("requires=" + new TreeSet<>(module.requires()));
      module.mainClass().ifPresent(main -> joiner.add("mainClass=" + main));
      return joiner.toString();
    }
    return String.valueOf(value);
  }
}
