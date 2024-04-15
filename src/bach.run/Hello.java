/*
 * Copyright (c) 2024 Christian Stein
 * Licensed under the Universal Permissive License v 1.0 -> https://opensource.org/license/upl
 */

class Hello {
  public static void main(String... args) {
    String name = args.length == 0 ? System.getProperty("user.name") : String.join(" ", args);
    System.out.printf("Hello %s!%n", name);
  }
}
