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

package test.base;

import java.lang.module.ModuleDescriptor;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/** Self-reflecting print support. */
public interface Printable {

  static List<String> print(Object object) {
    var lines = new ArrayList<String>();
    new Printable() {}.print(new Context(object, lines::add, new IdentityHashMap<>()), "");
    return List.copyOf(lines);
  }

  /** Print this instance using the given printer object. */
  default List<String> print() {
    var lines = new ArrayList<String>();
    print(lines::add);
    return List.copyOf(lines);
  }

  /** Print this instance using the given printer object. */
  default void print(Consumer<String> printer) {
    print(new Context(this, printer, new IdentityHashMap<>()), "");
  }

  final class Context {
    private final Object object;
    private final Consumer<String> printer;
    private final Map<Object, AtomicLong> printed;

    private Context(Object object, Consumer<String> printer, Map<Object, AtomicLong> printed) {
      this.object = object;
      this.printer = printer;
      this.printed = printed;
    }

    private Context nested(Object nested) {
      return new Context(nested, printer, printed);
    }
  }

  /** Recursive print method. */
  default void print(Context context, String indent) {
    var object = context.object;
    var printer = context.printer;
    var printed = context.printed;
    var caption = this == object ? printCaption() : object.getClass().getSimpleName();
    var counter = printed.get(this);
    if (counter != null) {
      var count = counter.getAndIncrement();
      printer.accept(String.format("%s# %s already printed (%d)", indent, caption, count));
      return;
    }
    printed.put(context.object, new AtomicLong(1));
    printer.accept(String.format("%s%s", indent, caption));
    try {
      var fields = object.getClass().getDeclaredFields();
      Arrays.sort(fields, Comparator.comparing(Field::getName));
      for (var field : fields) {
        if (field.isSynthetic()) continue;
        if (Modifier.isStatic(field.getModifiers())) continue;
        var name = field.getName();
        try {
          var method = object.getClass().getMethod(name); // mimic record component accessor
          if (!method.getReturnType().equals(field.getType())) continue;
          var value = method.invoke(object);
          print(context, indent, name, value);
        } catch (NoSuchMethodException e) {
          // continue
        }
      }
    } catch (ReflectiveOperationException e) {
      printer.accept(e.getMessage());
    }
  }

  /** Print the given name and its associated value. */
  private void print(Context context, String indent, String name, Object value) {
    var printer = context.printer;
    if (!printTest(name, value)) return;
    if (value instanceof Printable) {
      var type = value.getClass().getTypeName();
      printer.accept(String.format("  %s%s -> instance of %s", indent, name, type));
      ((Printable) value).print(context.nested(value), indent + "  ");
      return;
    }
    if (value instanceof Collection) {
      var collection = (Collection<?>) value;
      if (!collection.isEmpty()) {
        var first = collection.iterator().next();
        if (first instanceof Printable) {
          var size = collection.size();
          var type = value.getClass().getTypeName();
          printer.accept(String.format("  %s%s -> size=%d type=%s", indent, name, size, type));
          for (var element : collection) {
            if (element instanceof Printable) {
              ((Printable) element).print(context.nested(element), indent + "  ");
            } else printer.accept("Not printable element?! " + element.getClass());
          }
          return;
        }
      }
    }
    printer.accept(String.format("  %s%s = %s", indent, name, printBeautify(value)));
  }

  /** Return beautified String-representation of the given object. */
  default String printBeautify(Object object) {
    if (object == null) return "null";
    if (object.getClass().isArray()) {
      var length = Array.getLength(object);
      var joiner = new StringJoiner(", ", "[", "]");
      for (int i = 0; i < length; i++) joiner.add(printBeautify(Array.get(object, i)));
      return joiner.toString();
    }
    if (object instanceof String) return "\"" + object + "\"";
    if (object instanceof Path) {
      var string = String.valueOf(object);
      if (!string.isEmpty()) return string;
      return "\"" + object + "\" -> " + ((Path) object).toUri();
    }
    if (object instanceof ModuleDescriptor) {
      var module = (ModuleDescriptor) object;
      var joiner = new StringJoiner(", ", "module { ", " }");
      joiner.add("name: " + module.toNameAndVersion());
      joiner.add("requires: " + new TreeSet<>(module.requires()));
      module.mainClass().ifPresent(main -> joiner.add("mainClass: " + main));
      return joiner.toString();
    }
    return String.valueOf(object);
  }

  /** Return caption string of this object. */
  default String printCaption() {
    return getClass().getSimpleName();
  }

  /** Return {@code false} to prevent the named component from being printed. */
  default boolean printTest(String name, Object value) {
    return true;
  }
}
