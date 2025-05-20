/*
 * Copyright (c) 2025 Christian Stein
 * Licensed under the Universal Permissive License v 1.0 -> https://opensource.org/license/upl
 */

void main(String... args) {
  String name = args.length == 0 ? System.getProperty("user.name") : String.join(" ", args);
  IO.println("Ho %s!".formatted(name));
}
