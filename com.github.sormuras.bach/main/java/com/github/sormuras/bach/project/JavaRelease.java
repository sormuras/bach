package com.github.sormuras.bach.project;

import java.util.function.IntSupplier;

public record JavaRelease(int feature) implements IntSupplier {
  @Override
  public int getAsInt() {
    return feature;
  }
}
