package com.greetings;

import org.astro.World;

public class Main {
  public static void main(String[] args) {
    System.out.format("Greetings %s!%n", new World().name());
  }
}
