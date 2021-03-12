package com.github.sormuras.bach.api;

import com.github.sormuras.bach.Command;
import com.github.sormuras.bach.project.JavaStyle;
import com.github.sormuras.bach.tool.GoogleJavaFormat;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** Methods related to formatting Java source-code files. */
public interface JavaFormatterAPI extends API {

  enum Mode {
    APPLY,
    VERIFY
  }

  default List<Path> computeJavaSourceFilesToFormat() {
    return find(bach().folders().root(), JavaFormatterAPI::isJavaSourceFile);
  }

  default Command<?> computeJavaSourceFilesFormatCommand(
      List<Path> files, Mode mode, JavaStyle style) {
    if (style == JavaStyle.ANDROID || style == JavaStyle.GOOGLE) {
      var format = GoogleJavaFormat.install(bach());
      if (style == JavaStyle.ANDROID) format.add("--aosp");
      switch (mode) {
        case APPLY -> format = format.add("--replace");
        case VERIFY -> format = format.add("--dry-run").add("--set-exit-if-changed");
      }
      return format.addAll(files);
    }
    throw new UnsupportedOperationException("Unknown style: " + style);
  }

  default void formatJavaSourceFiles() {
    formatJavaSourceFiles(Mode.APPLY);
  }

  default void formatJavaSourceFiles(Mode mode) {
    var style = bach().project().spaces().style();
    if (style == JavaStyle.FREE) return;
    var files = computeJavaSourceFilesToFormat();
    if (files.isEmpty()) return;
    log("Format %s .java file%s: %s", files.size(), files.size() == 1 ? "" : "s", mode);
    var format = computeJavaSourceFilesFormatCommand(files, mode, style);
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
