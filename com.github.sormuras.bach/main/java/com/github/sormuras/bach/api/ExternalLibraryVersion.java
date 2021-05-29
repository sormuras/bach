package com.github.sormuras.bach.api;

import java.util.function.Supplier;

public record ExternalLibraryVersion(ExternalLibraryName name, String version) {

  public static ExternalLibraryVersion ofInfo(ProjectInfo.ExternalLibrary info) {
    return new ExternalLibraryVersion(info.name(), info.version());
  }

  public static ExternalLibraryVersion ofCommandLine(String supplier) {
    return null;
  }
}
