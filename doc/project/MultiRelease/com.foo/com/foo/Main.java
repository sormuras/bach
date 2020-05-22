package com.foo;

class Main {
  public static void main(String[] args) {
    System.out.println("com.foo.Main.main");
    System.out.println("  bar -> " + new org.bar.Bar());
    System.out.println("  baz -> " + new org.baz.Baz());
  }
}
