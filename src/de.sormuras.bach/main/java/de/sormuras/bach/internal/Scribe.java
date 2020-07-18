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

package de.sormuras.bach.internal;

import de.sormuras.bach.Project;
import de.sormuras.bach.project.Base;
import de.sormuras.bach.project.JavaRelease;
import de.sormuras.bach.project.Library;
import de.sormuras.bach.project.Link;
import de.sormuras.bach.project.MainSources;
import de.sormuras.bach.project.ModuleName;
import de.sormuras.bach.project.SourceDirectory;
import de.sormuras.bach.project.SourceDirectoryList;
import de.sormuras.bach.project.SourceUnit;
import de.sormuras.bach.project.SourceUnitMap;
import de.sormuras.bach.project.Sources;
import de.sormuras.bach.project.TestPreview;
import de.sormuras.bach.project.TestSources;
import java.lang.module.ModuleDescriptor;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/** A scribe knows how to transcribe itself into Java code. */
public interface Scribe {

  void scribe(Scroll scroll);

  static String escape(String string) {
    return string
        .replace("\\", "\\\\")
        .replace("\t", "\\t")
        .replace("\b", "\\b")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\f", "\\f")
        .replace("\"", "\\\"");
  }

  /** An extensible and mutable code builder. */
  class Scroll {

    private final StringBuilder text = new StringBuilder();
    private int depth = 0;
    private final String indent;
    private final String continuation;

    public Scroll(String indent) {
      this.indent = indent;
      this.continuation = indent;
    }

    public Scroll append(CharSequence sequence) {
      text.append(sequence);
      return this;
    }

    public Scroll addNewLine() {
      return append("\n").append(indent.repeat(depth));
    }

    public Scroll addNewLineAndContinue() {
      return addNewLine().append(continuation);
    }

    //
    // Dispatcher
    //

    public Scroll add(Object object) {
      if (object == null) return append("null");

      try {
        var add = getClass().getDeclaredMethod("add", object.getClass());
        add.invoke(this, object);
        return this;
      } catch (NoSuchMethodException e) {
        if (object instanceof Scribe) return add((Scribe) object);
        if (object instanceof Enum) return add((Enum<?>) object);
        if (object instanceof Path) return add((Path) object);
      } catch (ReflectiveOperationException e) {
        // fall-through
      }

      return append("new " + object.getClass().getCanonicalName() + "()");
    }

    public Scroll add(String factory, Object first, Object... more) {
      append(factory).append("(").add(first);
      for (var next : more) append(", ").add(next);
      return append(")");
    }

    //
    // Java
    //

    public Scroll add(Enum<?> constant) {
      Class<?> declaringClass = constant.getDeclaringClass();
      text.append(declaringClass.getEnclosingClass().getSimpleName())
          .append('.')
          .append(declaringClass.getSimpleName())
          .append('.')
          .append(constant.name());
      return this;
    }

    public Scroll add(Integer value) {
      text.append(value);
      return this;
    }

    public Scroll add(Long value) {
      text.append(value).append('L');
      return this;
    }

    public Scroll add(String string) {
      text.append('"').append(escape(string)).append('"');
      return this;
    }

    public Scroll add(Path path) {
      return add(path, true);
    }

    public Scroll add(Path path, boolean withPathOf) {
      var forward = path.toString().replace('\\', '/');
      return withPathOf ? append("Path.of(").add(forward).append(")") : add(forward);
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

    public Scroll add(List<?> list) {
      return addCollection("List.of", list);
    }

    public Scroll add(Set<?> set) {
      return addCollection("Set.of", set);
    }

    public Scroll addCollection(String factory, Collection<?> collection) {
      append(factory);
      var size = collection.size();
      if (size == 0) return append("()");
      if (size == 1) return add("", collection.stream().findFirst().get());
      append("(");
      collection.forEach(element -> addNewLineAndContinue().add(element).append(","));
      text.setLength(text.length() - 1); // remove last comma
      return append(")");
    }

    //
    // Bach
    //

    public Scroll add(Scribe scribe) {
      depth++;
      scribe.scribe(this);
      depth--;
      return this;
    }

    public Scroll add(Project project) {
      depth++;
      append("Project.of()");
      if (!project.base().isDefault()) addNewLine().add(".base", project.base());
      addNewLine().add(".name", project.name());
      addNewLine().add(".version", project.version().toString());
      addNewLine().add(".sources", project.sources());
      addNewLine().add(".library", project.library());
      depth--;
      return this;
    }

    public Scroll add(Base base) {
      if (base.isDefault()) return append("Base.of()");
      return base.isDefaultIgnoreBaseDirectory()
          ? add("Base.of", base.directory().toString().replace('\\', '/'))
          : add("new Base", base.directory(), base.libraries(), base.workspace());
    }

    public Scroll add(JavaRelease release) {
      var runtime = release == JavaRelease.ofRuntime();
      return append("JavaRelease.of").append(runtime ? "Runtime()" : "(" + release.feature() + ")");
    }

    public Scroll add(ModuleName module) {
      return add("ModuleName.of", module.name());
    }

    public Scroll add(Library library) {
      depth++;
      append("Library.of()");
      for (var name : library.toRequiredModuleNames()) addNewLine().add(".withRequires", name);
      for (var link : library.links().values()) addNewLine().add(".with", link);
      depth--;
      return this;
    }

    public Scroll add(Link link) {
      depth++;
      append("Link.of(");
      addNewLineAndContinue().add(link.module().name()).append(",");
      addNewLineAndContinue().add(link.uri());
      append(")");
      depth--;
      return this;
    }

    public Scroll add(Sources sources) {
      depth++;
      append("Sources.of()");
      addNewLine().add(".mainSources", sources.mainSources());
      addNewLine().add(".testSources", sources.testSources());
      addNewLine().add(".testPreview", sources.testPreview());
      depth--;
      return this;
    }

    public Scroll add(MainSources main) {
      depth++;
      append("MainSources.of()");
      for (var modifier : main.modifiers()) addNewLine().add(".with", modifier);
      addNewLine().add(".release", main.release());
      addNewLine().add(".units", main.units());
      depth--;
      return this;
    }

    public Scroll add(TestSources test) {
      depth++;
      append("TestSources.of()");
      addNewLine().add(".units", test.units());
      depth--;
      return this;
    }

    public Scroll add(TestPreview preview) {
      depth++;
      append("TestPreview.of()");
      addNewLine().add(".units", preview.units());
      depth--;
      return this;
    }

    public Scroll add(SourceUnitMap units) {
      depth++;
      append("SourceUnitMap.of()");
      units.map().values().stream().sorted().forEach(unit -> addNewLine().add(".with", unit));
      depth--;
      return this;
    }

    public Scroll add(SourceUnit unit) {
      depth++;
      append("new SourceUnit(");
      addNewLineAndContinue().add(unit.descriptor()).append(",");
      addNewLineAndContinue().add(unit.sources()).append(",");
      addNewLineAndContinue().add(unit.resources());
      append(")");
      depth--;
      return this;
    }

    public Scroll add(SourceDirectoryList directories) {
      depth++;
      append(directories.getClass().getSimpleName());
      var list = directories.list();
      if (list.size() == 1) {
        var directory = directories.first();
        if (directory.release() == 0) add(".of", directory.path());
        else add(".of", directory.path(), directory.release());
      } else {
        append(".of()");
        list.forEach(directory -> addNewLineAndContinue().add(".with", directory));
      }
      depth--;
      return this;
    }

    public Scroll add(SourceDirectory directory) {
      return add("new SourceDirectory", directory.path(), directory.release());
    }

    @Override
    public String toString() {
      return text.toString();
    }
  }
}
