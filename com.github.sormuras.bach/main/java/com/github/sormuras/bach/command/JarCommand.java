package com.github.sormuras.bach.command;

import com.github.sormuras.bach.Command;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The jar command creates an archive for classes and resources, and manipulates or restores
 * individual classes or resources from an archive.
 *
 * @param mode The main operation mode like {@code --create}, {@code --list}, or {@code --update}.
 * @param file Specifies the archive file name.
 * @param main Specifies the entry point for standalone applications bundled into a JAR file.
 * @param verbose Sends or prints verbose output to standard output.
 * @param additionals Aggregates additional command-line arguments.
 * @param files Specifies the classes and resources to operate on.
 * @see <a href="https://docs.oracle.com/en/java/javase/16/docs/specs/man/jar.html">The jar
 *     Command</a>
 */
public record JarCommand(
    ModeOption mode,
    FileOption file,
    MainClassOption main,
    VerboseOption verbose,
    AdditionalArgumentsOption additionals,
    FilesOption files)
    implements Command<JarCommand> {

  public JarCommand() {
    this(
        ModeOption.empty(),
        FileOption.empty(),
        MainClassOption.empty(),
        VerboseOption.empty(),
        AdditionalArgumentsOption.empty(),
        FilesOption.empty());
  }

  @Override
  public String name() {
    return "jar";
  }

  @Override
  public JarCommand option(Option option) {
    return new JarCommand(
        option instanceof ModeOption mode ? mode : mode,
        option instanceof FileOption file ? file : file,
        option instanceof MainClassOption main ? main : main,
        option instanceof VerboseOption verbose ? verbose : verbose,
        option instanceof AdditionalArgumentsOption additionals ? additionals : additionals,
        option instanceof FilesOption files ? files : files);
  }

  public JarCommand mode(String mode) {
    return option(new ModeOption(Optional.ofNullable(mode)));
  }

  public JarCommand file(Path file) {
    return option(new FileOption(Optional.ofNullable(file)));
  }

  public JarCommand main(String main) {
    return option(new MainClassOption(Optional.ofNullable(main)));
  }

  public JarCommand verbose(Boolean verbose) {
    return option(new VerboseOption(Optional.ofNullable(verbose)));
  }

  @Override
  public JarCommand additionals(AdditionalArgumentsOption additionals) {
    return option(additionals);
  }

  public JarCommand filesAdd(Path path) {
    return filesAdd(0, path);
  }

  public JarCommand filesAdd(int version, Path path) {
    return option(files.add(version, path));
  }

  @Override
  public List<String> toArguments() {
    var jar = Command.of(name());
    if (mode.isPresent()) jar = jar.add(mode.get());
    if (file.isPresent()) jar = jar.add("--file", file.get());
    if (main.isPresent()) jar = jar.add("--main-class", main.get());
    if (verbose.isTrue()) jar = jar.add("--verbose");
    //
    jar = jar.addAll(additionals.values());
    //
    if (files.isPresent()) {
      for (int version = 0; version <= Runtime.version().feature(); version++) {
        var targeting = files.targeting(version);
        if (targeting.isEmpty()) continue;
        if (version != 0) jar = jar.add("--release", version);
        for (var targeted : targeting) {
          for (var path : targeted.paths()) {
            jar = Files.isDirectory(path) ? jar.add("-C", path, ".") : jar.add(path);
          }
        }
      }
    }
    return jar.toArguments();
  }

  /** Main operation mode option. */
  public record ModeOption(Optional<String> value) implements Option.Value<String> {
    public static ModeOption empty() {
      return new ModeOption(Optional.empty());
    }
  }

  /** Archive file name option. */
  public record FileOption(Optional<Path> value) implements Option.Value<Path> {
    public static FileOption empty() {
      return new FileOption(Optional.empty());
    }
  }

  /** Entry point for standalone applications option. */
  public record MainClassOption(Optional<String> value) implements Option.Value<String> {
    public static MainClassOption empty() {
      return new MainClassOption(Optional.empty());
    }
  }

  /** Directories and regular files option. */
  public record FilesOption(List<TargetedPaths> values) implements Option.Values<TargetedPaths> {
    public static FilesOption empty() {
      return new FilesOption(List.of());
    }

    public FilesOption add(int version, Path path) {
      var values = new ArrayList<>(this.values);
      values.add(new TargetedPaths(version, List.of(path)));
      return new FilesOption(values);
    }

    public List<TargetedPaths> targeting(int version) {
      return values.stream().filter(targeted -> targeted.version() == version).toList();
    }
  }
}
