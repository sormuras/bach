package org.example;

class Main {
  public static void main(String... args) {
    System.out.println(Main.class + " in " + Main.class.getModule());
  }
}
