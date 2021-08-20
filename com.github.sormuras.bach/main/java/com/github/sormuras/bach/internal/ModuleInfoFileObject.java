package com.github.sormuras.bach.internal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.tools.SimpleJavaFileObject;

public class ModuleInfoFileObject extends SimpleJavaFileObject {
  ModuleInfoFileObject(Path path) {
    super(path.toUri(), Kind.SOURCE);
  }

  @Override
  public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
    return Files.readString(Path.of(uri));
  }
}
