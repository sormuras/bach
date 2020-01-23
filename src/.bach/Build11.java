// default package

/** Custom build program using {@code Bach${N}.java}. */
class Build11 {

  public static void main(String... args) {
    var bach = new Bach11();
    var project = bach.newProjectBuilder().setVersion("2.3.1").build();
    var plan = bach.newPlan(project);
    System.out.println(project);
    var count = plan.walk((indent, call) -> System.out.println(indent + "- " + call.toMarkdown()));
    System.out.printf("The generated call plan contains %d tool calls.%n", count);
    var context = new Bach11.Call.ExecutionContext();
    var configuration = new Bach11.Configuration(context, project, plan);
    var summary = bach.execute(configuration);
    System.out.println(summary.calls().size() + " calls executed:");
    summary.calls().forEach(call -> System.out.println(call.toMarkdown()));
  }
}
