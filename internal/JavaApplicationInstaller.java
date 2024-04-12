/*
 * Copyright (c) 2024 Christian Stein
 * Licensed under the Universal Permissive License v 1.0 -> https://opensource.org/license/upl
 */

package run.bach.internal;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.spi.ToolProvider;
import run.bach.Tool.Identifier;
import run.bach.ToolInstaller;
import run.bach.ToolProgram;

public record JavaApplicationInstaller(Identifier identifier, URI source) implements ToolInstaller {
  public JavaApplicationInstaller {
    Objects.requireNonNull(identifier);
    Objects.requireNonNull(source);
  }

  @Override
  public String namespace() {
    return identifier.namespace();
  }

  @Override
  public String name() {
    return identifier.name();
  }

  @Override
  public String version() {
    return identifier.version().orElseGet(() -> String.valueOf(source.toString().hashCode()));
  }

  @Override
  public ToolProvider installInto(Path folder) {
    var filename = Path.of(source.getPath()).getFileName().toString();
    var target = folder.resolve(filename);
    if (!Files.exists(target)) {
      try {
        download(target, source);
      } catch (Exception exception) {
        throw new RuntimeException(exception);
      }
    }
    if (filename.endsWith(".jar")) return ToolProgram.java("-jar", target.toString());
    if (filename.endsWith(".java")) return ToolProgram.java(target.toString());
    throw new IllegalArgumentException("Unsupported program type: " + source);
  }
}
