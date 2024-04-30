/*
 * Copyright (c) 2024 Christian Stein
 * Licensed under the Universal Permissive License v 1.0 -> https://opensource.org/license/upl
 */

package run.bach.internal;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.spi.ToolProvider;
import run.bach.Tool.Identifier;
import run.bach.ToolInstaller;
import run.bach.ToolProgram;

public record JavaToolInstaller(String name, String version, Properties properties)
    implements ToolInstaller {
  public JavaToolInstaller {
    Objects.requireNonNull(name);
    Objects.requireNonNull(version);
    Objects.requireNonNull(properties);
  }

  @Override
  public String namespace() {
    return properties.getProperty("@namespace", "");
  }

  @Override
  public String name() {
    return properties.getProperty("@name", name);
  }

  @Override
  public String version() {
    return properties.getProperty("@version", version);
  }

  @Override
  public ToolProvider install(Path into) throws Exception {
    String launcher = null;
    var jarFiles = new ArrayList<Path>();
    var javaFiles = new ArrayList<Path>();
    for (var key : properties.stringPropertyNames()) {
      if (key.startsWith("@")) {
        if (key.equals("@java")) {
          launcher =
              properties
                  .getProperty(key)
                  .strip()
                  .replace("{{tool.home}}", into.toString())
                  .replace('/', File.separatorChar)
                  .replace('\\', File.separatorChar);
        }
        continue;
      }
      var value = properties.getProperty(key);
      var target = into.resolve(key);
      if (value.startsWith("https:")) download(target, URI.create(value));
      if (value.startsWith("string:")) Files.writeString(target, value.substring(7));
      if (PathSupport.isJarFile(target)) jarFiles.add(target);
      if (PathSupport.isJavaFile(target)) javaFiles.add(target);
    }
    if (launcher != null) return ToolProgram.java(launcher.split("\\s+"));
    if (javaFiles.size() == 1) return ToolProgram.java(javaFiles.getFirst().toString());
    if (jarFiles.size() == 1) return ToolProgram.java("-jar", jarFiles.getFirst().toString());
    throw new IllegalStateException("No @java launcher specified, nor a sole Java program found.");
  }

  public static JavaToolInstaller ofPropertiesFile(Path file) {
    var properties = PathSupport.properties(file);
    var name = file.getFileName().toString();
    var extension = ".properties";
    var parent = file.resolveSibling(name.substring(0, name.length() - extension.length()));
    var identifier = Identifier.of(parent.toString());
    return new JavaToolInstaller(identifier.name(), identifier.version().orElse("0"), properties);
  }

  public static Optional<ToolInstaller> find(String string) {
    try {
      // ".bach/tool/foo@bar.properties" | .bach/tool/ "string" .properties
      var candidates = List.of(string, ".bach/tool/" + string + ".properties");
      for (var candidate : candidates) {
        var file = Path.of(candidate);
        if (PathSupport.isPropertiesFile(file)) {
          var installer = JavaToolInstaller.ofPropertiesFile(file);
          return Optional.of(installer);
        }
      }
      var list =
          PathSupport.list(Path.of(".bach/tool"), PathSupport::isPropertiesFile).stream()
              .filter(path -> PathSupport.name(path, "").startsWith(string))
              .toList();
      if (!list.isEmpty()) {
        if (list.size() > 1) {
          System.out.println("Selecting first tool properties file of:");
          list.forEach(System.out::println);
        }
        var file = list.getFirst();
        var installer = JavaToolInstaller.ofPropertiesFile(file);
        return Optional.of(installer);
      }
    } catch (Exception ignore) {
    }
    return Optional.empty();
  }
}
