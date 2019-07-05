package de.sormuras.bach;

public class AllTests {
  public static void main(String[] args) {
    System.out.println(Project.class + " is in " + Project.class.getModule());
    ProjectTests.main(args);
    UtilTests.main(args);
  }
}
