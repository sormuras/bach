package com.github.sormuras.bach.lookup;

/** Thrown when an error occurs looking up an external module. */
public class LookupException extends RuntimeException {

  @java.io.Serial private static final long serialVersionUID = 1302244222050872687L;

  public LookupException(String module) {
    super("Module " + module + " not found");
  }
}
