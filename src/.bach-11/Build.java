// default package

/** Custom build program using {@code Bach.java} API. */
class Build {

  public static void main(String... args) {
    // Bach.main(args); // Delegate to Bach.java's default main program.

    new Bach().build(project -> project.version("123")).assertSuccessful();
  }
}
