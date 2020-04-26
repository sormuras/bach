package com.greetings;

public class Main {

  public static void main(String[] args) {
    System.out.format("Greetings from %s!%n", Main.class.getModule());
  }
}
