import java.nio.file.Path;

class Build {
  public static void main(String[] args) {
    System.out.println("Build Simplicissimus");
    System.out.println(Path.of("").toUri());
    Bach.main(args);
  }
}
