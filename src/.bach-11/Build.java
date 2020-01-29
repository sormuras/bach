// default package

/** Custom build program using {@code Bach${N}.java}. */
class Build {

  public static void main(String... args) {
    var summary =
        Bach.build(
            project ->
                project
                    .version("47.11")
                    .requires("org.junit.jupiter.api", "5.6.0")
                    .requires("org.junit.jupiter.params", "5.6.0"));
    if (summary.throwable() != null) {
      summary.throwable().printStackTrace();
      System.exit(1);
    }
  }
}
