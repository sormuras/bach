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
import de.sormuras.bach.project.CodeSpaces;
import de.sormuras.bach.project.CodeUnit;
import de.sormuras.bach.project.CodeUnits;
import de.sormuras.bach.project.JavaRelease;
import de.sormuras.bach.project.Library;
import de.sormuras.bach.project.Link;
import de.sormuras.bach.project.MainSpace;
import de.sormuras.bach.project.SourceFolder;
import de.sormuras.bach.project.SourceFolders;
import de.sormuras.bach.project.TestSpace;
import de.sormuras.bach.project.TestSpacePreview;
import java.lang.System.Logger.Level;
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
        System.getLogger(getClass().getName()).log(Level.WARNING, "add failed: " + object, e);
      }

      return append("new " + object.getClass().getCanonicalName() + "()");
    }

    public Scroll addNew(Class<?> type, Object... arguments) {
      return addCall("new " + type.getSimpleName(), arguments);
    }

    public Scroll addOf(Class<?> type, Object... arguments) {
      return addCall(type.getSimpleName() + ".of", arguments);
    }

    public Scroll addCall(String factory, Object... arguments) {
      append(factory).append("(");
      if (arguments.length > 0) {
        add(arguments[0]);
        for (int i = 1; i < arguments.length; i++) append(", ").add(arguments[i]);
      }
      return append(")");
    }

    //
    // Java
    //

    public Scroll add(Class<?> type) {
      text.append(type.getSimpleName());
      return this;
    }

    public Scroll add(Enum<?> constant) {
      var declaringClass = constant.getDeclaringClass();
      var enclosingClass = declaringClass.getEnclosingClass();
      if (enclosingClass != null) text.append(enclosingClass.getSimpleName()).append('.');
      text.append(declaringClass.getSimpleName()).append('.').append(constant.name());
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
      return withPathOf ? addOf(Path.class, forward) : add(forward);
    }

    public Scroll add(ModuleDescriptor module) {
      addCall("ModuleDescriptor.newModule", module.name());
      depth++;
      module.version().ifPresent(v -> addNewLineAndContinue().addCall(".version", v));
      module.mainClass().ifPresent(c -> addNewLineAndContinue().addCall(".mainClass", c));
      module.requires().forEach(r -> addNewLineAndContinue().addCall(".requires", r.name()));
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
      if (size == 1) return addCall("", collection.stream().findFirst().get());
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
      addOf(Project.class);
      if (!project.base().isDefault()) addNewLine().addCall(".base", project.base());
      addNewLine().addCall(".name", project.name());
      addNewLine().addCall(".version", project.version().toString());
      addNewLine().addCall(".spaces", project.spaces());
      addNewLine().addCall(".library", project.library());
      depth--;
      return this;
    }

    public Scroll add(Base base) {
      if (base.isDefault()) return addOf(Base.class);
      return base.isDefaultIgnoreBaseDirectory()
          ? addOf(Base.class, base.directory().toString().replace('\\', '/'))
          : addNew(Base.class, base.directory(), base.libraries(), base.workspace());
    }

    public Scroll add(JavaRelease release) {
      if (release == JavaRelease.ofRuntime()) return add(JavaRelease.class).append(".ofRuntime()");
      return addOf(JavaRelease.class, release.feature());
    }

    public Scroll add(Library library) {
      depth++;
      addOf(Library.class);
      for (var name : library.toRequiredModuleNames()) addNewLine().addCall(".withRequires", name);
      for (var link : library.links().values()) addNewLine().addCall(".with", link);
      depth--;
      return this;
    }

    public Scroll add(Link link) {
      depth++;
      add(Link.class).append(".of(");
      /*addNewLineAndContinue().*/ add(link.module()).append(",");
      addNewLineAndContinue().add(link.uri());
      append(")");
      depth--;
      return this;
    }

    public Scroll add(CodeSpaces sources) {
      depth++;
      addOf(CodeSpaces.class);
      addNewLine().addCall(".main", sources.main());
      addNewLine().addCall(".test", sources.test());
      addNewLine().addCall(".preview", sources.preview());
      depth--;
      return this;
    }

    public Scroll add(MainSpace main) {
      depth++;
      addOf(MainSpace.class);
      for (var feature : main.features()) addNewLine().addCall(".with", feature);
      addNewLine().addCall(".release", main.release());
      addNewLine().addCall(".units", main.units());
      depth--;
      return this;
    }

    public Scroll add(TestSpace test) {
      depth++;
      addOf(TestSpace.class);
      addNewLine().addCall(".units", test.units());
      depth--;
      return this;
    }

    public Scroll add(TestSpacePreview preview) {
      depth++;
      addOf(TestSpacePreview.class);
      addNewLine().addCall(".units", preview.units());
      depth--;
      return this;
    }

    public Scroll add(CodeUnits units) {
      depth++;
      addOf(CodeUnits.class);
      units.map().values().stream().sorted().forEach(unit -> addNewLine().addCall(".with", unit));
      depth--;
      return this;
    }

    public Scroll add(CodeUnit unit) {
      depth++;
      append("new ").add(CodeUnit.class).append("(");
      addNewLineAndContinue().add(unit.descriptor()).append(",");
      addNewLineAndContinue().add(unit.sources()).append(",");
      addNewLineAndContinue().add(unit.resources());
      append(")");
      depth--;
      return this;
    }

    public Scroll add(SourceFolders directories) {
      depth++;
      var list = directories.list();
      if (list.size() == 1) {
        var directory = directories.first();
        if (directory.release() == 0) addOf(SourceFolders.class, directory.path());
        else addOf(SourceFolders.class, directory.path(), directory.release());
      } else {
        addOf(SourceFolders.class);
        list.forEach(directory -> addNewLineAndContinue().addCall(".with", directory));
      }
      depth--;
      return this;
    }

    public Scroll add(SourceFolder directory) {
      return addNew(SourceFolder.class, directory.path(), directory.release());
    }

    @Override
    public String toString() {
      return text.toString();
    }
  }
}
