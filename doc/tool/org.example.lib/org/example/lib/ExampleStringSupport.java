package org.example.lib;

import java.util.StringJoiner;

/** String-related helpers. */
public class ExampleStringSupport {
  /**
   * @return a string composed of all given strings
   * @param strings to join
   */
  public static String join(String... strings) {
    var joiner = new StringJoiner(" ", "(", ")");
    for (var string : strings) joiner.add(string);
    return joiner.toString();
  }
}
