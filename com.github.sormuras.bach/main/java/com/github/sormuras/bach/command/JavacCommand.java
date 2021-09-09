package com.github.sormuras.bach.command;

import com.github.sormuras.bach.Command;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * The javac command reads Java declarations and compiles them into class files.
 *
 * @param release Compile source code according to the rules of the Java programming language for
 *     the specified Java SE release, generating class files which target that release.
 * @param modules Compiles those source files in the named modules that are newer than the
 *     corresponding files in the output directory.
 * @param moduleSourcePathPatterns Specifies where to find source files when compiling code in
 *     multiple modules: a module-pattern form, in which the source path for each module is
 *     specified by a pattern.
 * @param moduleSourcePathSpecifics Specifies where to find source files when compiling code in
 *     multiple modules: a module-specific form, in which a package path is given for each module
 *     containing code to be compiled.
 * @param modulePathsOption Specifies where to find application modules.
 * @param verbose Outputs messages about what the compiler is doing. Messages include information
 *     about each class loaded and each source file compiled.
 * @param outputDirectoryForClasses Specify where to place generated class files.
 * @param additionals Aggregates additional command-line arguments.
 * @see <a href="https://docs.oracle.com/en/java/javase/16/docs/specs/man/javac.html">javac</a>
 */
public record JavacCommand(
    ReleaseOption release,
    ModulesOption modules,
    ModuleSourcePathPatternsOption moduleSourcePathPatterns,
    ModuleSourcePathSpecificsOption moduleSourcePathSpecifics,
    ModulePathsOption modulePathsOption,
    VerboseOption verbose,
    OutputDirectoryOption outputDirectoryForClasses,
    AdditionalArgumentsOption additionals)
    implements Command<JavacCommand> {

  public JavacCommand() {
    this(
        ReleaseOption.empty(),
        ModulesOption.empty(),
        ModuleSourcePathPatternsOption.empty(),
        ModuleSourcePathSpecificsOption.empty(),
        ModulePathsOption.empty(),
        VerboseOption.empty(),
        OutputDirectoryOption.empty(),
        AdditionalArgumentsOption.empty());
  }

  @Override
  public String name() {
    return "javac";
  }

  @Override
  public JavacCommand option(Option option) {
    return new JavacCommand(
        option instanceof ReleaseOption release ? release : release,
        option instanceof ModulesOption modules ? modules : modules,
        option instanceof ModuleSourcePathPatternsOption paths ? paths : moduleSourcePathPatterns,
        option instanceof ModuleSourcePathSpecificsOption paths ? paths : moduleSourcePathSpecifics,
        option instanceof ModulePathsOption paths ? paths : modulePathsOption,
        option instanceof VerboseOption verbose ? verbose : verbose,
        option instanceof OutputDirectoryOption directory ? directory : outputDirectoryForClasses,
        option instanceof AdditionalArgumentsOption additionals ? additionals : additionals);
  }

  public JavacCommand release(Integer release) {
    return option(new ReleaseOption(Optional.ofNullable(release)));
  }

  public JavacCommand modules(String... modules) {
    return modules(List.of(modules));
  }

  public JavacCommand modules(List<String> modules) {
    return option(new ModulesOption(List.copyOf(modules)));
  }

  public JavacCommand moduleSourcePathPatterns(String... patterns) {
    return option(new ModuleSourcePathPatternsOption(List.of(patterns)));
  }

  public JavacCommand moduleSourcePathAddPattern(String segment) {
    return option(moduleSourcePathPatterns.add(segment));
  }

  public JavacCommand moduleSourcePathAddSpecific(String module, Path path, Path... more) {
    return option(moduleSourcePathSpecifics.withModuleSpecificForm(module, path, more));
  }

  public JavacCommand modulePaths(Path... paths) {
    return option(new ModulePathsOption(List.of(paths)));
  }

  public JavacCommand modulePathsAdd(Path path, Path... more) {
    return option(modulePathsOption.add(path, more));
  }

  public JavacCommand verbose(Boolean verbose) {
    return option(new VerboseOption(Optional.ofNullable(verbose)));
  }

  public JavacCommand outputDirectoryForClasses(Path path) {
    return option(new OutputDirectoryOption(Optional.ofNullable(path)));
  }

  @Override
  public JavacCommand additionals(AdditionalArgumentsOption additionals) {
    return option(additionals);
  }

  @Override
  public List<String> toArguments() {
    var javac = Command.of(name());
    if (release.isPresent()) javac = javac.add("--release", release.get());
    if (modules.isPresent()) javac = javac.add("--module", modules.join(","));
    if (moduleSourcePathPatterns.isPresent()) {
      javac = javac.add("--module-source-path", moduleSourcePathPatterns.join());
    }
    for (var specific : moduleSourcePathSpecifics.values()) {
      javac = javac.add("--module-source-path", specific);
    }
    if (modulePathsOption.isPresent()) {
      javac = javac.add("--module-path", modulePathsOption.join(File.pathSeparator));
    }
    if (verbose.isTrue()) javac = javac.add("-verbose");
    if (outputDirectoryForClasses.isPresent()) {
      javac = javac.add("-d", outputDirectoryForClasses.get());
    }
    //
    javac = javac.addAll(additionals.values());
    //
    return javac.toArguments();
  }

  /** Java SE release feature version option. */
  public record ReleaseOption(Optional<Integer> value) implements Option.Value<Integer> {
    public static ReleaseOption empty() {
      return new ReleaseOption(Optional.empty());
    }
  }
}
