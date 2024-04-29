/*
 * Copyright (c) 2024 Christian Stein
 * Licensed under the Universal Permissive License v 1.0 -> https://opensource.org/license/upl
 */

package run.bach;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;
import run.bach.internal.JavaApplicationInstaller;
import run.bach.internal.JavaToolInstaller;

/** An interface for installers to fetch required files and compose them into a tool provider. */
public interface ToolInstaller {
  /**
   * {@return the default namespace used by this tool installer}
   *
   * @see Tool.Identifier#namespace()
   */
  default String namespace() {
    var type = getClass();
    var module = type.getModule();
    return module.isNamed() ? module.getName() : type.getPackageName();
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
   * {@return the version for this tool installer}
   *
   * @see Tool.Identifier#version()
   */
  String version();

  ToolProvider install(Path into) throws Exception;

  default Tool install() {
    return install(Mode.DEFAULT);
  }

  default Tool install(Mode mode) {
    var identifier = Tool.Identifier.of(namespace(), name(), version());
    var folders = Bach.Folders.ofCurrentWorkingDirectory();
    return install(folders.tool(), identifier, mode);
  }

  private Tool install(Path base, Tool.Identifier identifier, Mode mode) {
    // resolve installation folder
    var path = Path.of(identifier.namespace());
    if (path.getRoot() != null) {
      path = path.getRoot().relativize(path); // strip root component from namespace
    }
    var folder = base.resolve(path).resolve(identifier.toNameAndVersion()).normalize();
    return switch (mode) {
      case INSTALL_IMMEDIATE -> Tool.of(identifier, install(this, folder));
      case INSTALL_ON_DEMAND -> Tool.of(identifier, () -> install(this, folder));
    };
  }

  default void download(Path target, URI source) throws IOException {
    if (!Files.exists(target)) {
      try (var stream =
          source.getScheme().startsWith("http")
              ? source.toURL().openStream()
              : Files.newInputStream(Path.of(source))) {
        var parent = target.getParent();
        if (parent != null) Files.createDirectories(parent);
        Files.copy(stream, target, StandardCopyOption.REPLACE_EXISTING);
      }
    }
    // TODO Verify target bits.
  }

  private static ToolProvider install(ToolInstaller installer, Path directory) {
    try {
      Files.createDirectories(directory);
      var lockfile = directory.resolve(".lockfile");
      try {
        try {
          Files.createFile(lockfile);
        } catch (FileAlreadyExistsException exception) {
          while (Files.exists(lockfile)) {
            // noinspection BusyWait
            Thread.sleep(1234);
          }
        }
        return installer.install(directory);
      } finally {
        Files.deleteIfExists(lockfile);
      }
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  }

  static Optional<ToolInstaller> find(String string) {
    return JavaToolInstaller.find(string).or(() -> JavaApplicationInstaller.find(string));
  }

  static ToolInstaller ofJavaApplication(String id, String uri) {
    var identifier = Tool.Identifier.of(id);
    var source = URI.create(uri);
    return new JavaApplicationInstaller(identifier, source);
  }

  static Finder finder(Mode mode) {
    return new Finder(List.of(), mode, Bach.Folders.ofCurrentWorkingDirectory().tool());
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
     * @see Tool#of(String, Tool.ToolProviderSupplier)
     */
    INSTALL_ON_DEMAND;

    /** Default to on-demand installation of tools. */
    public static final Mode DEFAULT = INSTALL_ON_DEMAND;
  }

  record Finder(List<Tool> tools, Mode mode, Path installationHomeDirectory) implements ToolFinder {
    public Finder with(ToolInstaller installer) {
      var identifier = Tool.Identifier.of(installer);
      return with(identifier, installer);
    }

    public Finder with(String id, ToolInstaller installer) {
      var identifier = Tool.Identifier.of(id);
      return with(identifier, installer);
    }

    public Finder withJavaApplication(String id, String uri) {
      var identifier = Tool.Identifier.of(id);
      var source = URI.create(uri);
      var installer = new JavaApplicationInstaller(identifier, source);
      return with(identifier, installer);
    }

    private Finder with(Tool.Identifier identifier, ToolInstaller installer) {
      var tool = installer.install(installationHomeDirectory, identifier, mode);
      var tools = Stream.concat(tools().stream(), Stream.of(tool)).toList();
      return new Finder(tools, mode, installationHomeDirectory);
    }
  }
}
