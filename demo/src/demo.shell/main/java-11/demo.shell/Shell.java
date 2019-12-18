package demo.shell;

public class Shell {
  @Override
  public String toString() {
    return String.format("Shell(11 on %d)", Runtime.version().feature());
  }
}
