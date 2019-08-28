package de.sormuras.bach;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public class Bach {

  public static String VERSION = "2-ea";

  public static void main(String... args) throws Exception {
    try (var stream = Bach.class.getModule().getResourceAsStream("de/sormuras/bach/banner.txt")) {
      if (stream != null) {
        var lines = new BufferedReader(new InputStreamReader(stream)).lines();
        var banner = lines.collect(Collectors.joining(System.lineSeparator()));
        System.out.println(banner + " " + VERSION);
      } else {
        System.out.printf("Bach %s (member of %s)%n", VERSION, Bach.class.getModule());
      }
    }
  }
}
