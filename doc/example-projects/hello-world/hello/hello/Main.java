package hello;

class Main {
  public static void main(String... args) {
    System.out.printf("%s in %s%n", Main.class, Main.class.getModule());
    System.out.printf("hello -> %s%n", ModuleLayer.boot().findModule("hello").orElseThrow().getDescriptor());
    System.out.printf("world -> %s%n", ModuleLayer.boot().findModule("world").orElseThrow().getDescriptor());
  }
}
