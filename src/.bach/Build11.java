// default package

/** Custom build program using Bach. */
class Build11 {

  public static void main(String... args) {
    var bach = new Bach11();
    var project = bach.newProject();
    // var project = bach.newProjectBuilder(Path.of("")).setVersion(Version.parse("2.3.1")).build();
    var plan = bach.newPlan(project);
    System.out.println(project);
    var count = plan.walk((indent, call) -> System.out.println(indent + "- " + call.toMarkdown()));
    System.out.printf("The generated call plan contains %d tool calls.%n", count);
  }
}
