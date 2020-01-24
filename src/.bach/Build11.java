// default package

/** Custom build program using {@code Bach${N}.java}. */
class Build11 {

  public static void main(String... args) {
    var bach = new Bach11();
    var project = bach.newProjectBuilder().setVersion("2.3.1").build();
    bach.build(project);
  }
}
