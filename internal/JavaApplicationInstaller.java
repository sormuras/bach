/*
 * Copyright (c) 2024 Christian Stein
 * Licensed under the Universal Permissive License v 1.0 -> https://opensource.org/license/upl
 */

package run.bach.internal;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.spi.ToolProvider;
import run.bach.Tool;
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
  public ToolProvider install(Path into) throws Exception {
    var filename = Path.of(source.getPath()).getFileName().toString();
    var target = into.resolve(filename);
    if (!Files.exists(target)) download(target, source);
    if (filename.endsWith(".jar")) return ToolProgram.java("-jar", target.toString());
    if (filename.endsWith(".java")) return ToolProgram.java(target.toString());
    throw new IllegalArgumentException("Unsupported program type: " + source);
  }

  public static Optional<JavaApplicationInstaller> find(String string) {
    try {
      var source = new URI(string);
      if (!source.isAbsolute()) {
        return Optional.empty();
      }
      /* Test for Maven 2 coordinates */ {
        var pattern =
            Pattern.compile(
                "https://[^/]+"
                    + "/maven2"
                    + "/(?<namespace>.+)"
                    + "/(?<name>[^/]+)"
                    + "/(?<version>[^/]+)"
                    + "/.+");
        var matcher = pattern.matcher(string);
        if (matcher.matches()) {
          var identifier =
              Tool.Identifier.of(
                  matcher.group("namespace").replace('/', '.'),
                  matcher.group("name"),
                  matcher.group("version"));
          return Optional.of(new JavaApplicationInstaller(identifier, source));
        }
      }
      /* Test for GitHub releases */ {
        var pattern =
            Pattern.compile(
                "https://github\\.com"
                    + "/(?<namespace>[^/]+)"
                    + "/(?<name>[^/]+)"
                    + "/releases/download"
                    + "/(?<version>[^/]+)"
                    + "/.+");
        var matcher = pattern.matcher(string);
        if (matcher.matches()) {
          var identifier =
              Tool.Identifier.of(
                  matcher.group("namespace"), matcher.group("name"), matcher.group("version"));
          return Optional.of(new JavaApplicationInstaller(identifier, source));
        }
      }
      /* Test for GitHub raw resources */ {
        var pattern =
            Pattern.compile(
                "https://raw\\.githubusercontent\\.com"
                    + "/(?<namespace>[^/]+)"
                    + "/(?<name>[^/]+)"
                    + "/(?<version>[^/]+)"
                    + "/.+");
        var matcher = pattern.matcher(string);
        if (matcher.matches()) {
          var identifier =
              Tool.Identifier.of(
                  matcher.group("namespace"), matcher.group("name"), matcher.group("version"));
          return Optional.of(new JavaApplicationInstaller(identifier, source));
        }
      }
      /* Still here? Compute an identifier based on uri's properties. */ {
        var namespace = source.getHost() != null ? source.getHost() : "application";
        var hash = Math.abs(string.hashCode());
        var name = String.valueOf(hash);
        try {
          var path = source.getPath();
          if (path != null && !path.isBlank()) {
            var file = Path.of(path).normalize().getFileName();
            if (file != null) {
              name = file.toString();
            }
          }
        } catch (Exception ignore) {
        }
        var version = Integer.toHexString(hash);
        var identifier = Tool.Identifier.of(namespace, name, version);
        return Optional.of(new JavaApplicationInstaller(identifier, source));
      }
    } catch (URISyntaxException exception) {
      return Optional.empty();
    }
  }
}
