// default package

/** Custom build program using {@code Bach.java} API. */
class Build {

  public static void main(String... args) {
    // Bach.main(args); // Delegate to Bach.java's default main program.

    var buildSummary = new Bach().build(project -> project.version("1-ea"));
    buildSummary.assertSuccessful();
  }
}
