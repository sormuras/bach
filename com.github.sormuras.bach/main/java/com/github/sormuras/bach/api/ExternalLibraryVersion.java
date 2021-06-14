package com.github.sormuras.bach.api;

import com.github.sormuras.bach.internal.Strings;

public record ExternalLibraryVersion(ExternalLibraryName name, String version) {

  public static ExternalLibraryVersion of(String string) {
    var split = string.split("=");
    var name = Strings.toEnum(ExternalLibraryName.class, split[0]);
    var version = split[1];
    return new ExternalLibraryVersion(name, version);
  }
}
