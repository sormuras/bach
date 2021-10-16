package com.github.sormuras.bach;

public class FindException extends RuntimeException {
  @java.io.Serial private static final long serialVersionUID = 1863359159669939232L;

  public FindException(Class<?> type, String name) {
    super("Could not find instance of " + type + " by name: " + name);
  }

  public FindException(String message) {
    super(message);
  }
}
