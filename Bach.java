/*
 * Copyright (c) 2024 Christian Stein
 * Licensed under the Universal Permissive License v 1.0 -> https://opensource.org/license/upl
 */

package run.bach;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.List;
import java.util.Map;
import run.bach.internal.ModuleSourcePathSupport;

/** Defines Bach's constants and helpers. */
public record Bach(Folders folders) {
  /**
   * Well-known directories and files.
   *
   * @param root the root or home directory that usually contains a {@code .bach} subdirectory
   * @param out used to store generated files into, defaults to {@code ${root}.bach/out}
   * @param var used to store cacheable file into, defaults to {@code ${root}.bach/var}
   */
  public record Folders(Path root, Path out, Path var) {
    /** {@code .bach} */
    public static Folders ofCurrentWorkingDirectory() {
      return Folders.of(Path.of(""));
    }

    /** {@code C:\Users\${user.name}\.bach} */
    public static Folders ofUserHomeDirectory() {
      return Folders.of(Path.of(System.getProperty("user.home", "")));
    }

    /** {@code C:\Users\${user.name}\AppData\Local\Temp\.bach} */
    public static Folders ofTemporaryDirectory() {
      return Folders.of(Path.of(System.getProperty("java.io.tmpdir", "")));
    }

    /**
     * {@code C:\Users\${user.name}\AppData\Local\Temp\${prefix}17762885918332141894\.bach}
     *
     * @see Files#createTempDirectory(String, FileAttribute[])
     */
    public static Folders ofTemporaryDirectory(String prefix) {
      try {
        return Folders.of(Files.createTempDirectory(prefix));
      } catch (Exception exception) {
        throw new RuntimeException(exception);
      }
    }

    public static Folders of(Path root) {
      var normalized = root.normalize();
      var out = normalized.resolve(".bach", "out");
      var var = normalized.resolve(".bach", "var");
      return new Folders(normalized, out, var);
    }

    public Path root(String first, String... more) {
      return root.resolve(first, more);
    }

    public Path out(String first, String... more) {
      return out.resolve(first, more);
    }

    public Path var(String first, String... more) {
      return var.resolve(first, more);
    }

    public Path tools() {
      return var("cache", "tools");
    }

    public Path tools(String first, String... more) {
      return tools().resolve(first, more);
    }

    public String computeModuleSourcePath(String module, String info) {
      return computeModuleSourcePath(module, root(info));
    }

    public String computeModuleSourcePath(String module, Path... paths) {
      return computeModuleSourcePath(Map.of(module, List.of(paths)));
    }

    public String computeModuleSourcePath(Map<String, List<Path>> modules) {
      return String.join(File.pathSeparator, ModuleSourcePathSupport.compute(modules, false));
    }
  }
}
