package hello;

class Main {
  public static void main(String... args) {
    System.out.printf("%s in %s%n", Main.class, Main.class.getModule());
  }
}
