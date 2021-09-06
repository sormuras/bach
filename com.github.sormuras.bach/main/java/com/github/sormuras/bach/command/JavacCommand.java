package com.github.sormuras.bach.command;

import java.util.List;
import java.util.Optional;

/**
 * The javac command reads Java declarations and compiles them into class files.
 *
 * @param release Compile source code according to the rules of the Java programming language for
 *     the specified Java SE release, generating class files which target that release.
 * @param modules Compiles those source files in the named modules that are newer than the
 *     corresponding files in the output directory.
 * @param verbose Outputs messages about what the compiler is doing. Messages include information
 *     about each class loaded and each source file compiled.
 * @param additionals Aggregates additional command-line arguments.
 */
public record JavacCommand(
    ReleaseOption release,
    ModulesOption modules,
    VerboseOption verbose,
    AdditionalArgumentsOption additionals)
    implements Command<JavacCommand> {

  public JavacCommand() {
    this(
        ReleaseOption.empty(),
        ModulesOption.empty(),
        VerboseOption.empty(),
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
        option instanceof VerboseOption verbose ? verbose : verbose,
        option instanceof AdditionalArgumentsOption additionals ? additionals : additionals);
  }

  public JavacCommand release(Integer release) {
    return option(new ReleaseOption(Optional.ofNullable(release)));
  }

  @Override
  public JavacCommand additionals(AdditionalArgumentsOption additionals) {
    return option(additionals);
  }

  public JavacCommand modules(String... modules) {
    return option(new ModulesOption(List.of(modules)));
  }

  public JavacCommand verbose(Boolean verbose) {
    return option(new VerboseOption(Optional.ofNullable(verbose)));
  }

  @Override
  public List<String> toArguments() {
    var javac = Command.of(name());
    if (release.isPresent()) javac = javac.add("--release", release.get());
    if (modules.isPresent()) javac = javac.add("--module", modules.join(","));
    if (verbose.isTrue()) javac = javac.add("--verbose");
    javac = javac.addAll(additionals.values());
    return javac.toArguments();
  }

  /** Java SE release feature version option. */
  public record ReleaseOption(Optional<Integer> value) implements Option.Value<Integer> {
    public static ReleaseOption empty() {
      return new ReleaseOption(Optional.empty());
    }
  }
}
