import java.util.function.BiFunction;

class PrinterFunction implements BiFunction<Bach, Project, Integer> {
  @Override
  public Integer apply(Bach bach, Project project) {
    bach.log("%s %s", project.name(), project.version());
    return 0;
  }
}
