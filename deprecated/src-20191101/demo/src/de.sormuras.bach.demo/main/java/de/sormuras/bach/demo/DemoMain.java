package de.sormuras.bach.demo;

class DemoMain {
  public static void main(String[] args) throws ReflectiveOperationException {
    var type = DemoMain.class;
    System.out.println("Method " + type.getMethod("main", String[].class));
    System.out.println("  is declared in " + type + ",");
    System.out.println("    which resides in " + type.getPackage() + ",");
    System.out.println("      that is a member of " + type.getModule() + "lib");
    assert "t".equals(type.getModule().getName()) : "Expected module name to be: t";
  }
}
