package com.github.sormuras.bach;

public record Tweak(Bach bach, Call call) {

  @FunctionalInterface
  public interface Handler {
    Call handle(Tweak tweak);
  }
}
