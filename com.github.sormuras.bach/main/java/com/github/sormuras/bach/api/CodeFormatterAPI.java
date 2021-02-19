package com.github.sormuras.bach.api;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.tool.GoogleJavaFormat;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** Methods related to formatting code files. */
public interface CodeFormatterAPI {

  Bach bach();

  default void formatJavaSourceFiles() {
    var files = find(bach().base().directory(), CodeFormatterAPI::isJavaSourceFile);
    if (files.isEmpty()) return;
    bach().debug("Format %s .java file%s", files.size(), files.size() == 1 ? "" : "s");
    var format = new GoogleJavaFormat().add("--replace").add("", files.toArray());
    bach().run(format).requireSuccessful();
  }

  default void verifyFormatOfJavaSourceFiles() {
    var files = find(bach().base().directory(), CodeFormatterAPI::isJavaSourceFile);
    if (files.isEmpty()) return;
    bach().debug("Verify format of %s .java file%s", files.size(), files.size() == 1 ? "" : "s");
    var verify =
        new GoogleJavaFormat()
            .add("--dry-run")
            .add("--set-exit-if-changed")
            .add("", files.toArray());
    bach().run(verify).requireSuccessful();
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
