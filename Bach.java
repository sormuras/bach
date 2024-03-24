/*
 * Copyright (c) 2024 Christian Stein
 * Licensed under the Universal Permissive License v 1.0 -> https://opensource.org/license/upl
 */

package run.bach;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import run.bach.internal.ModuleSourcePathSupport;

/** Defines Bach's constants and helpers. */
public final class Bach {
  /**
   * Well-known directories and files.
   *
   * @param root the root or home directory that usually contains a {@code .bach} subdirectory
   * @param out used to store generated files into, defaults to {@code ${root}.bach/out}
   * @param var used to store cacheable file into, defaults to {@code ${root}.bach/var}
   */
  public record Folders(Path root, Path out, Path var) {
    public static Folders ofCurrentWorkingDirectory() {
      return Folders.of(Path.of(""));
    }

    public static Folders of(Path root) {
      var normalized = root.normalize();
      var out = normalized.resolve(".bach", "out");
      var var = normalized.resolve(".bach", "var");
      return new Folders(normalized, out, var);
    }

    public Path root(String first, String... more) {
      return root.resolve(Path.of(first, more));
    }

    public Path out(String first, String... more) {
      return out.resolve(Path.of(first, more));
    }

    public Path var(String first, String... more) {
      return var.resolve(Path.of(first, more));
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

  private Bach() {}
}
