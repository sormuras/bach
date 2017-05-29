package de.sormuras.solartools;

import java.io.Serializable;

/** Main class showing all available solar-related tools. */
public class Main implements Serializable {
  /**
   * Entry-point that prints all available solar-related tools to standard out.
   *
   * @param args program arguments
   */
  public static void main(String... args) {
    System.out.println(Main.class + " in " + Main.class.getModule());
  }
}
