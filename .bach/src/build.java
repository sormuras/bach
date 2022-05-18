import com.github.sormuras.bach.Main;
import com.github.sormuras.bach.Printer;

class build {

  public static void main(String... args) {
    System.setProperty("java.util.logging.config.file", ".bach/logging.properties");

    System.out.println(">>");

    Main.bach(Printer.ofSystem(), args).run("build"); // resolves to "project/build"

    System.out.println("<<");
  }
}
