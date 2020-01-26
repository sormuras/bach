// default package

/** Custom build program using {@code Bach${N}.java}. */
class Build {

  public static void main(String... args) {
    var summary = Bach.build(project -> project.version("47.11"));
    if (summary.throwable() != null) throw new Error(summary.throwable().getMessage());
  }
}
