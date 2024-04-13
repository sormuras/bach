/*
 * Copyright (c) 2024 Christian Stein
 * Licensed under the Universal Permissive License v 1.0 -> https://opensource.org/license/upl
 */

package run.bach;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;
import run.bach.internal.JavaApplicationInstaller;

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

  ToolProvider installInto(Path folder);

  default Tool install() {
    return install(Mode.DEFAULT);
  }

  default Tool install(Mode mode) {
    var version = version();
    var identifier = Tool.Identifier.of(namespace(), name(), version);
    var folders = Bach.Folders.ofCurrentWorkingDirectory();
    var folder = resolveInstallationFolder(folders.tools(), identifier);
    return switch (mode) {
      case INSTALL_IMMEDIATE -> Tool.of(identifier, installInto(folder, this));
      case INSTALL_ON_DEMAND -> Tool.of(identifier, () -> installInto(folder, this));
    };
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

  private Path resolveInstallationFolder(Path base, Tool.Identifier identifier) {
    return base.resolve(identifier.namespace()).resolve(identifier.toNameAndVersion()).normalize();
  }

  private static ToolProvider installInto(Path folder, ToolInstaller installer) {
    try {
      Files.createDirectories(folder);
      var lockfile = folder.resolve(".lockfile");
      try {
        try {
          Files.createFile(lockfile);
        } catch (FileAlreadyExistsException exception) {
          while (Files.exists(lockfile)) {
            // noinspection BusyWait
            Thread.sleep(1234);
          }
        }
        return installer.installInto(folder);
      } finally {
        Files.deleteIfExists(lockfile);
      }
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  }

  static Optional<ToolInstaller> find(String string) {
    try {
      var source = new URI(string);
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
        var name = Integer.toHexString(hash);
        var version = String.valueOf(hash);
        var identifier = Tool.Identifier.of(namespace, name, version);
        return Optional.of(new JavaApplicationInstaller(identifier, source));
      }
    } catch (URISyntaxException exception) {
      return Optional.empty();
    }
  }

  static ToolInstaller ofJavaApplication(String id, String uri) {
    var identifier = Tool.Identifier.of(id);
    var source = URI.create(uri);
    return new JavaApplicationInstaller(identifier, source);
  }

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
      var folder = installer.resolveInstallationFolder(installationHomeDirectory, identifier);
      var tool =
          switch (mode) {
            case INSTALL_IMMEDIATE -> Tool.of(identifier, installInto(folder, installer));
            case INSTALL_ON_DEMAND -> Tool.of(identifier, () -> installInto(folder, installer));
          };
      var tools = Stream.concat(tools().stream(), Stream.of(tool)).toList();
      return new Finder(tools, mode, installationHomeDirectory);
    }
  }
}
