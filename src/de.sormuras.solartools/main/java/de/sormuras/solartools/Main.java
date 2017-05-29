package de.sormuras.solartools;

import java.io.Serializable;

public class Main implements Serializable {
  public static void main(String... args) {
    System.out.println(Main.class + " in " + Main.class.getModule());
  }
}
