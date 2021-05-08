package com.github.sormuras.bach.api;

import java.util.Locale;

public enum ExternalLibraryName {
  JUNIT;

  public static ExternalLibraryName ofCli(String cli) {
    return valueOf(cli.toUpperCase(Locale.ROOT).replace('-', '_'));
  }
}
