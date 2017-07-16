package com.greetings;

public interface Greeter {

  default String world() {
    return "world";
  }

  String greet(String name);
}
