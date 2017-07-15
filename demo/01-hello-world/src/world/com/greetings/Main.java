package com.greetings;

import java.util.ServiceLoader;

public class Main {

  public static void main(String... args) {
    System.out.println("\n");
    System.out.println("Greetings from 01-hello-world demo!");
    // System.out.println("Calling hello: " + new com.hello.Hello().hello());
    for (Greeter greeter : ServiceLoader.load(Greeter.class)) {
      System.out.println("  " + greeter.greet("Joe"));
    }
    System.out.println("\n");
  }
}
