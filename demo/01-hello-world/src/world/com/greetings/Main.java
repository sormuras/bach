package com.greetings;

import java.util.ServiceLoader;

public class Main {

  public static void main(String... args) {
    System.out.println("\n");
    System.out.println("Greetings from 01-hello-world demo!");
    System.out.println("\n");
    System.out.println("Entry-point: " + Main.class + " in " + Main.class.getModule());
    System.out.print("StackWalker: ");
    StackWalker.getInstance().forEach(System.out::println);
    System.out.println("\n");
    for (Greeter greeter : ServiceLoader.load(Greeter.class)) {
      System.out.println(
          "  '"
              + greeter.greet(greeter.world())
              + "' from "
              + greeter.getClass()
              + " in "
              + greeter.getClass().getModule());
    }
    System.out.println();
  }
}
