package com.greetings;

public class Main {

  public static void main(String[] args) {
    System.out.format("Greetings World! (%s)%n", Main.class.getModule());
  }
}
