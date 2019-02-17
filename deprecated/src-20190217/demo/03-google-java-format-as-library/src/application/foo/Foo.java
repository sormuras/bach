package foo;

import com.google.googlejavaformat.java.Formatter;

public class Foo {

  public static void main(String... args) throws Exception {
    var sourceString = "package foo; enum Bar {A,B,C}";
    var formattedSource = new Formatter().formatSource(sourceString);
    System.out.println(formattedSource);
  }
}
