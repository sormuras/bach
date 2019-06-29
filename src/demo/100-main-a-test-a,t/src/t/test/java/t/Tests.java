package t;

public class Tests {
  public static void main(String... args) throws Exception {
    System.out.println("Method " + Tests.class.getMethod("main", String[].class));
    System.out.println("  is declared in " + Tests.class + ",");
    System.out.println("    which resides in " + Tests.class.getPackage() + ",");
    System.out.println("      that is a member of " + Tests.class.getModule() + ".");
    assert "t".equals(Tests.class.getModule().getName());
  }
}
