package application.api.internal;

import application.api.Plugin;

public class Reverse implements Plugin {

  @Override
  public String apply(String s) {
    return new StringBuilder(s).reverse().toString();
  }
}
