// default package

/** Custom build program using {@code Bach${N}.java}. */
class Build {

  public static void main(String... args) {
    Bach.build(project -> project.version("47.11"));
  }
}
