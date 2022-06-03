import com.github.sormuras.bach.Bach;

class build {

  public static void main(String... args) {
    System.setProperty("java.util.logging.config.file", ".bach/logging.properties");

    System.out.println(">>");

    Bach.ofSystem(args).run("build"); // resolves to "project/build"

    System.out.println("<<");
  }
}
