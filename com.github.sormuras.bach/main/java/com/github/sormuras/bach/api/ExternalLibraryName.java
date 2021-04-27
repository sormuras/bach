package com.github.sormuras.bach.api;

import java.util.Locale;

public enum ExternalLibraryName {
  JUNIT;

  public static ExternalLibraryName ofCli(String cli) {
    return valueOf(cli.toUpperCase(Locale.ROOT).replace('-', '_'));
  }

  public static String toCli(ExternalLibraryName externalLibraryName) {
    return externalLibraryName.name().toLowerCase(Locale.ROOT).replace('_', '-');
  }

  public String cli() {
    return toCli(this);
  }
}
