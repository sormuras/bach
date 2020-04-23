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

import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Path;
import java.util.List;

/** A {@code jar} call configuration creating an archive for classes and resources. */
public /*static*/ final class JavaArchiveTool extends Tool {

  /** Main operation modes. */
  public enum Operation {
    /** Creates the archive. */
    CREATE,
    /** Generates index information for the specified JAR file. */
    GENERATE_INDEX,
    /** Lists the table of contents for the archive. */
    LIST,
    /** Updates an existing JAR file. */
    UPDATE,
    /** Extracts the named (or all) files from the archive. */
    EXTRACT,
    /** Prints the module descriptor or automatic module name. */
    DESCRIBE_MODULE
  }

  JavaArchiveTool(List<? extends Option> options) {
    super("jar", options);
  }

  /** When using the jar command, you must specify the operation for it to perform. */
  public static final class PerformOperation implements Option {

    private final Operation mode;
    private final List<String> more;

    public PerformOperation(Operation mode, String... more) {
      this.mode = mode;
      this.more = List.of(more);
    }

    public Operation mode() {
      return mode;
    }

    public List<String> more() {
      return more;
    }

    @Override
    public void visit(Arguments arguments) {
      var key = "--" + mode.toString().toLowerCase().replace('_', '-');
      var value = more.isEmpty() ? "" : "=" + more.get(0);
      arguments.add(key + value);
    }
  }

  /** Specify the archive file name. */
  public static final class ArchiveFile extends KeyValueOption<Path> {

    public ArchiveFile(Path file) {
      super("--file", file);
    }
  }

  /** Change to the specified directory and include the following files. */
  public static final class ChangeDirectory extends KeyValueOption<Path> {

    public ChangeDirectory(Path value) {
      super("-C", value);
    }

    @Override
    public void visit(Arguments arguments) {
      arguments.add("-C", value(), ".");
    }
  }

  /** The application entry point for stand-alone applications bundled into a modular JAR file. */
  public static final class MainClass extends KeyValueOption<String> {

    public MainClass(String className) {
      super("--main-class", className);
    }
  }

  /** Set the version of this module. */
  public static final class ModuleVersion extends KeyValueOption<Version> {

    public ModuleVersion(Version version) {
      super("--module-version", version);
    }
  }

  /** Place all following files in a versioned directory {@code META-INF/versions/VERSION/}. */
  public static final class MultiReleaseVersion implements Option {

    private final int version;

    public MultiReleaseVersion(int version) {
      this.version = version;
    }

    public int version() {
      return version;
    }

    @Override
    public void visit(Arguments arguments) {
      arguments.add("--release", version);
    }
  }

  /** Generate verbose output on standard output. */
  public static final class Verbose extends ObjectArrayOption<String> {
    public Verbose() {
      super("--verbose");
    }
  }
}
