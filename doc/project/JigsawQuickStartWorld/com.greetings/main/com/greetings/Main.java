package com.greetings;

import org.astro.World;

/** The main program of the greetings module. */
public class Main {

  public static void main(String[] args) {
    System.out.format("Greetings %s from (%s)!%n", new World().name(), Main.class.getModule());
    System.out.format("World resides in %s.%n", World.class.getModule());
  }
}
