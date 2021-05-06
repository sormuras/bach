package com.github.sormuras.bach.api;

import java.util.function.Supplier;

public record ExternalLibraryVersion(ExternalLibraryName name, String version) {

  public static ExternalLibraryVersion ofInfo(ProjectInfo.ExternalLibrary info) {
    return new ExternalLibraryVersion(info.name(), info.version());
  }

  public static ExternalLibraryVersion ofCommandLine(Supplier<String> supplier) {
    var name = ExternalLibraryName.ofCli(supplier.get());
    var version = supplier.get();
    return new ExternalLibraryVersion(name, version);
  }
}
