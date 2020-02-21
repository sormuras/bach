package com.greetings;

import org.astro.World;

public class Main {

  public static void main(String[] args) {
    System.out.format("Greetings %s! (%s)%n", World.name(), Main.class.getModule());
    System.out.format("World from %s%n", World.class.getModule());
  }
}
