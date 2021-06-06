package com.github.sormuras.bach;

import com.github.sormuras.bach.internal.Strings;
import java.util.List;
import java.util.Optional;

public record Command(Name name, List<String> arguments) {
  public enum Name {
    NOOP,
    PRINT_VERSION,
    PRINT_HELP,
    PRINT_HELP_EXTRA,
    PRINT_MODULES,
    PRINT_DECLARED_MODULES,
    PRINT_EXTERNAL_MODULES,
    PRINT_SYSTEM_MODULES,
    LOAD_EXTERNAL_MODULE,
    LOAD_MISSING_EXTERNAL_MODULES,
    PRINT_TOOLS,
    DESCRIBE_TOOL,
    RUN_TOOL
  }

  public static Command of() {
    return new Command(Name.NOOP, List.of());
  }

  public static Command of(String string, String... args) {
    return new Command(Strings.toEnum(Name.class, string), List.of(args));
  }

  public static Optional<Name> findName(String string) {
    return switch (string) {
      case "--version" -> Optional.of(Name.PRINT_VERSION);
      case "--help" -> Optional.of(Name.PRINT_HELP);
      case "--help-extra" -> Optional.of(Name.PRINT_HELP_EXTRA);
      case "--print-modules" -> Optional.of(Name.PRINT_MODULES);
      case "--print-declared-modules" -> Optional.of(Name.PRINT_DECLARED_MODULES);
      case "--print-external-modules" -> Optional.of(Name.PRINT_EXTERNAL_MODULES);
      case "--print-system-modules" -> Optional.of(Name.PRINT_SYSTEM_MODULES);
      case "--load-external-module" -> Optional.of(Name.LOAD_EXTERNAL_MODULE);
      case "--load-missing-external-modules" -> Optional.of(Name.LOAD_MISSING_EXTERNAL_MODULES);
      case "--print-tools" -> Optional.of(Name.PRINT_TOOLS);
      case "--describe-tool" -> Optional.of(Name.DESCRIBE_TOOL);
      case "--run", "--tool" -> Optional.of(Name.RUN_TOOL);
      default -> Optional.empty();
    };
  }
}
