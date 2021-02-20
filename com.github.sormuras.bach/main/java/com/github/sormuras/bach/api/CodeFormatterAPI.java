package com.github.sormuras.bach.api;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Command;
import com.github.sormuras.bach.tool.GoogleJavaFormat;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** Methods related to formatting code files. */
public interface CodeFormatterAPI {

  enum Mode {
    APPLY,
    VERIFY
  }

  Bach bach();

  default List<Path> computeJavaSourceFilesToFormat() {
    return find(bach().base().directory(), CodeFormatterAPI::isJavaSourceFile);
  }

  default Command<?> computeJavaSourceFilesFormatCommand(List<Path> files, Mode mode) {
    var format = GoogleJavaFormat.install(bach());
    switch (mode) {
      case APPLY -> format = format.add("--replace");
      case VERIFY -> format = format.add("--dry-run").add("--set-exit-if-changed");
    }
    return format.add("", files.toArray());
  }

  default void formatJavaSourceFiles() {
    formatJavaSourceFiles(Mode.APPLY);
  }

  default void formatJavaSourceFiles(Mode mode) {
    var files = computeJavaSourceFilesToFormat();
    if (files.isEmpty()) return;
    bach().debug("Format %s .java file%s: %s", files.size(), files.size() == 1 ? "" : "s", mode);
    var format = computeJavaSourceFilesFormatCommand(files, mode);
    bach().run(format).requireSuccessful();
  }

  private static boolean isJavaSourceFile(Path path) {
    return Files.isRegularFile(path) && path.getFileName().toString().endsWith(".java");
  }

  private static List<Path> find(Path start, Predicate<Path> filter) {
    var files = new ArrayList<Path>();
    try (var stream = Files.walk(start)) {
      stream.filter(filter).forEach(files::add);
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
    return List.copyOf(files);
  }
}
