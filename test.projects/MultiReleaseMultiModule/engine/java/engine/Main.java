package engine;

import api.Api;

class Main {
  public static void main(String... args) {
    System.out.println(Api.class);
    System.out.println(OverlaySingleton.INSTANCE.display());
  }
}
