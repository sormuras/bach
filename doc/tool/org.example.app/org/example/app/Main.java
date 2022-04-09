package org.example.app;

import org.example.lib.ExampleStringSupport;

public class Main {
  public static void main(String... args) {
    System.out.println("Example application is running.");
    System.out.println("" + Main.class.getProtectionDomain().getCodeSource().getLocation());
    System.out.println("  " + Main.class.getModule());
    System.out.println("    " + Main.class.getPackage());
    System.out.println("      " + Main.class);
    System.out.println(" args -> " + ExampleStringSupport.join(args));
  }
}
