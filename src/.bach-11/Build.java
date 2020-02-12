// default package

/** Custom build program using {@code Bach.java} API. */
class Build {

  public static void main(String... args) {
    new Bach().build(project -> project.version("1-ea")).assertSuccessful();
  }
}
