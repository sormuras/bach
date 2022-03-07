public final class bach {
  public static void main(String... args) {
    String name = args.length == 0 ? System.getProperty("user.name") : String.join(" ", args);
    System.out.printf("Hello %s!%n", name);
  }
}
