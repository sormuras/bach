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

import static java.lang.String.format;

import java.lang.module.ModuleDescriptor;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/** A scribe knows how to transcribe itself into Java code. */
public interface Scribe {

  void scribe(Scroll scroll);

  static String escape(String string) {
    return string.replace("\\", "\\\\")
        .replace("\t", "\\t")
        .replace("\b", "\\b")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\f", "\\f")
        .replace("\"", "\\\"");
  }

  /** An extensible and mutable code builder. */
  class Scroll {

    public static Scroll of() {
      return new Scroll("  ");
    }

    private final StringBuilder text = new StringBuilder();
    private int depth = 0;
    private final String indent;
    private final String continuation;

    public Scroll(String indent) {
      this.indent = indent;
      this.continuation = indent;
    }

    public Scroll append(String string) {
      text.append(string);
      return this;
    }

    public Scroll append(String format, Object... args) {
      if (args.length == 0) return append(format);
      return append(format(format, args));
    }

    public Scroll add(Object object) {
      return append(transcribe(object));
    }

    public Scroll add(String factory, Object first, Object... more) {
      append(factory).append("(");
      if (first instanceof Scribe) add((Scribe) first);
      else add(first);
      for (var next : more) {
        append(", ");
        if (next instanceof Scribe) {
          add((Scribe) next);
          continue;
        }
        add(next);
      }
      return append(")");
    }

    public Scroll add(Scribe scribe) {
      depth++;
      scribe.scribe(this);
      depth--;
      return this;
    }

    public Scroll add(List<?> list) {
      return addCollection("List.of", list);
    }

    public Scroll add(Set<?> set) {
      return addCollection("Set.of", set);
    }

    private Scroll addCollection(String factory, Collection<?> collection) {
      append(factory);
      var size = collection.size();
      if (size == 0) return append("()");
      if (size == 1) return add("", collection.stream().findFirst().get());
      append("(");
      collection.forEach(o -> addNewLineAndContinue().append(transcribe(o)).append(","));
      text.setLength(text.length() - 1); // remove last comma
      return append(")");
    }

    public Scroll add(ModuleDescriptor module) {
      add("ModuleDescriptor.newModule", module.name());
      depth++;
      module.version().ifPresent(v -> addNewLineAndContinue().add(".version", v));
      module.mainClass().ifPresent(c -> addNewLineAndContinue().add(".mainClass", c));
      module.requires().forEach(r -> addNewLineAndContinue().add(".requires", r.name()));
      addNewLineAndContinue().append(".build()");
      depth--;
      return this;
    }

    public Scroll addNewLine() {
      return append("\n").append(indent.repeat(depth));
    }

    public Scroll addNewLineAndContinue() {
      return addNewLine().append(continuation);
    }

    private String transcribe(Object object) {
      if (object == null) return "null";
      if (object == this) return "/*this*/";
      if (object instanceof Enum) return transcribe((Enum<?>) object);
      if (object instanceof Number) return "" + object;
      if (object instanceof String) return transcribe((String) object);
      if (object instanceof Path) return transcribe((Path) object);
      return "new " + object.getClass().getCanonicalName() + "()";
    }

    private String transcribe(Enum<?> constant) {
      Class<?> declaring = constant.getDeclaringClass();
      var type = declaring.getEnclosingClass();
      return String.join(".", type.getSimpleName(), declaring.getSimpleName(), constant.name());
    }

    private String transcribe(String string) {
      return format("\"%s\"", escape(string));
    }

    private String transcribe(Path path) {
      return format("Path.of(%s)", transcribe(path.toString().replace('\\', '/')));
    }

    @Override
    public String toString() {
      return text.toString();
    }
  }
}
