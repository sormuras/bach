package com.github.sormuras.bach.lookup;

import java.util.List;

public record ModuleMetadata(String name, long size, List<Checksum> checksums) {
  public record Checksum(String algorithm, String value) {}
}
