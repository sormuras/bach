/*
 * Copyright (c) 2024 Christian Stein
 * Licensed under the Universal Permissive License v 1.0 -> https://opensource.org/license/upl
 */

package run.bach;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;

/** An interface for installers to fetch required files and compose them into a tool provider. */
@FunctionalInterface
public interface ToolInstaller {
  ToolProvider install(String version, Path into);

  /**
   * {@return the default namespace used by this tool installer}
   *
   * @see Tool.Identifier#namespace()
   */
  default String namespace() {
    return getClass().getPackageName().replace('.', '/');
  }

  /**
   * {@return the default name used by this tool installer}
   *
   * @see Tool.Identifier#name()
   */
  default String name() {
    return getClass().getSimpleName().toLowerCase(Locale.ROOT);
  }

  /**
   * {@return the default version for this tool installer}
   *
   * @implNote this default implementation throws {@link UnsupportedOperationException}
   * @see Tool.Identifier#version()
   */
  default String version() {
    throw new UnsupportedOperationException("No default version defined: " + getClass());
  }

  default Installation installation(String version) {
    var identifier = Tool.Identifier.of(namespace(), name(), version);
    return new Installation(identifier, this);
  }

  default Tool tool(String version) {
    var identifier = Tool.Identifier.of(namespace(), name(), version);
    var folders = Bach.Folders.ofCurrentWorkingDirectory();
    var into = resolveInstallationDirectory(folders.tools(), identifier);
    var provider = install(version, into);
    return Tool.of(identifier, provider);
  }

  default void download(Path target, URI source) {
    if (!Files.exists(target)) {
      try (var stream = source.toURL().openStream()) {
        var parent = target.getParent();
        if (parent != null) Files.createDirectories(parent);
        Files.copy(stream, target, StandardCopyOption.REPLACE_EXISTING);
      } catch (Exception exception) {
        throw new RuntimeException("Download failed: " + source, exception);
      }
    }
    // TODO Verify target bits.
  }

  default Path resolveInstallationDirectory(Path base, Tool.Identifier identifier) {
    return base.resolve(identifier.namespace()).resolve(identifier.toNameAndVersion()).normalize();
  }

  record Installation(Tool.Identifier identifier, ToolInstaller installer) {}

  static Finder finder(Mode mode) {
    return new Finder(List.of(), mode, Bach.Folders.ofCurrentWorkingDirectory().tools());
  }

  /** Tool installation mode. */
  enum Mode {
    /**
     * Install tools eagerly while composing the finder by using the provider-taking tool factory.
     *
     * @see Tool#of(String, ToolProvider)
     */
    INSTALL_IMMEDIATE,

    /**
     * Install tools lazily only when running them by using the supplier-taking tool factory.
     *
     * @see Tool#of(String, Supplier)
     */
    INSTALL_ON_DEMAND
  }

  record Finder(List<Tool> tools, Mode mode, Path installationHomeDirectory) implements ToolFinder {
    public Finder with(String id, Supplier<ToolInstaller> supplier) {
      return with(id, supplier.get());
    }

    public Finder with(String id, ToolInstaller installer) {
      var identifier = Tool.Identifier.of(id);
      var installation = new Installation(identifier, installer);
      return with(installation);
    }

    public Finder withJavaApplication(String id, String uri) {
      var identifier = Tool.Identifier.of(id);
      var source = URI.create(uri);
      var installer = new ApplicationInstaller(identifier.namespace(), identifier.name(), source);
      var installation = new Installation(identifier, installer);
      return with(installation);
    }

    public Finder with(Installation installation) {
      var identifier = installation.identifier();
      var installer = installation.installer();
      var version = identifier.version().orElse(installer.version());
      var into = installer.resolveInstallationDirectory(installationHomeDirectory, identifier);
      var tool =
          switch (mode) {
            case INSTALL_IMMEDIATE -> Tool.of(identifier, installer.install(version, into));
            case INSTALL_ON_DEMAND -> Tool.of(identifier, () -> installer.install(version, into));
          };
      var tools = Stream.concat(tools().stream(), Stream.of(tool)).toList();
      return new Finder(tools, mode, installationHomeDirectory);
    }
  }

  record ApplicationInstaller(String namespace, String name, URI source) implements ToolInstaller {
    @Override
    public ToolProvider install(String version, Path into) {
      var filename = Path.of(source.getPath()).getFileName().toString();
      var target = into.resolve(filename);
      System.out.println("target = " + target.toUri());
      if (!Files.exists(target)) {
        try {
          download(target, source);
        } catch (Exception exception) {
          throw new RuntimeException(exception);
        }
      }
      if (filename.endsWith(".jar")) return ToolProgram.java("-jar", target.toString());
      if (filename.endsWith(".java")) return ToolProgram.java(target.toString());
      throw new IllegalArgumentException("Unsupported program type: " + filename);
    }
  }
}
