package com.github.sormuras.bach.api;

import java.util.Locale;

public record ExternalLibraryVersion(ExternalLibraryName name, String version) {

  public static ExternalLibraryVersion ofInfo(ProjectInfo.ExternalLibrary info) {
    return new ExternalLibraryVersion(info.name(), info.version());
  }

  public static ExternalLibraryVersion of(String string) {
    var split = string.split("=");
    var name = split[0].toUpperCase(Locale.ROOT).replace('-', '_');
    var version = split[1];
    return new ExternalLibraryVersion(ExternalLibraryName.valueOf(name), version);
  }
}
