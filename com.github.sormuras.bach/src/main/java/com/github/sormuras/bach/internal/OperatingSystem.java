package com.github.sormuras.bach.internal;

import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public record OperatingSystem(Name name, Architecture architecture) {

  public enum Name {
    ANY(".*"),
    LINUX("^linux.*"),
    WINDOWS("^windows.*");

    private final String identifier;
    private final Pattern pattern;

    Name(String regex) {
      this.identifier = name().toLowerCase(Locale.ROOT);
      this.pattern = Pattern.compile(regex);
    }

    boolean matches(String string) {
      return pattern.matcher(string).matches();
    }

    @Override
    public String toString() {
      return identifier;
    }

    public static Name ofSystem() {
      return of(System.getProperty("os.name", "").toLowerCase(Locale.ROOT));
    }

    public static Name of(String string) {
      var normalized = string.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "");
      return Stream.of(WINDOWS, LINUX)
          .filter(platform -> platform.matches(normalized))
          .findFirst()
          .orElseThrow(() -> new IllegalArgumentException("Unknown platform: " + string));
    }
  }

  public enum Architecture {
    ANY(".*"),
    X86_32("^(x8632|x86|i[3-6]86|ia32|x32)$"),
    X86_64("^(x8664|amd64|ia32e|em64t|x64)$");

    private final String identifier;
    private final Pattern pattern;

    Architecture(String regex) {
      this.identifier = name().toLowerCase(Locale.ROOT);
      this.pattern = Pattern.compile(regex);
    }

    boolean matches(String string) {
      return pattern.matcher(string).matches();
    }

    @Override
    public String toString() {
      return identifier;
    }

    public static Architecture ofSystem() {
      return of(System.getProperty("os.arch", ""));
    }

    public static Architecture of(String string) {
      var normalized = string.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "");
      return Stream.of(X86_64, X86_32)
          .filter(architecture -> architecture.matches(normalized))
          .findFirst()
          .orElseThrow(() -> new IllegalArgumentException("Unknown architecture: " + string));
    }
  }

  public static final OperatingSystem ANY = new OperatingSystem(Name.ANY, Architecture.ANY);

  public static final OperatingSystem SYSTEM = new OperatingSystem(Name.ofSystem(), Architecture.ofSystem());

  public static OperatingSystem of(String classifier) {
    if (classifier == null || classifier.isEmpty()) return ANY;
    var index = classifier.indexOf('-');
    return index == -1
        ? new OperatingSystem(Name.of(classifier), Architecture.ANY)
        : new OperatingSystem(
            Name.of(classifier.substring(0, index)),
            Architecture.of(classifier.substring(index + 1)));
  }
}
