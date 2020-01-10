import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

class Generate {
  public static void main(String[] args) throws Exception {
    int balloons = 99;
    var base = Path.of("");

    // parent pom
    var parent = new ArrayList<String>();
    parent.addAll(
        List.of(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
            "<project>",
            "  <modelVersion>4.0.0</modelVersion>",
            "  <groupId>de.sormuras.bach.doc</groupId>",
            "  <artifactId>bach-doc-ninetynine</artifactId>",
            "  <version>99</version>",
            "  <packaging>pom</packaging>",
            "",
            "  <properties>",
            "    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>",
            "  </properties>",
            "",
            "  <build>",
            "    <pluginManagement>",
            "      <plugins>",
            "        <plugin>",
            "          <groupId>org.apache.maven.plugins</groupId>",
            "          <artifactId>maven-compiler-plugin</artifactId>",
            "          <version>3.8.1</version>",
            "          <configuration>",
            "            <release>11</release>",
            "          </configuration>",
            "        </plugin>",
            "      </plugins>",
            "    </pluginManagement>",
            "  </build>",
            "",
            "  <modules>"));
    for (int i = 0; i <= balloons; i++) {
      parent.add(String.format("    <module>m%02di</module>", i));
    }
    parent.add("  </modules>");
    parent.add("</project>");
    Files.write(base.resolve("pom.xml"), parent);

    // 00
    var m00i = Files.createDirectories(base.resolve("m00i/src/main/java"));
    Files.write(m00i.resolve("module-info.java"), List.of("module m00i {}", ""));
    Files.write(
        base.resolve("m00i/pom.xml"),
        List.of(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
            "<project>",
            "  <modelVersion>4.0.0</modelVersion>",
            "  <parent>",
            "    <groupId>de.sormuras.bach.doc</groupId>",
            "    <artifactId>bach-doc-ninetynine</artifactId>",
            "    <version>99</version>",
            "  </parent>",
            "",
            "  <artifactId>m00i</artifactId>",
            "</project>"));

    // 01..99
    for (int i = 1; i <= balloons; i++) {
      var name = String.format("m%02di", i);
      var folder = Files.createDirectories(base.resolve(name + "/src/main/java"));
      var lines = new ArrayList<String>();
      lines.add("module " + name + " {");
      for (int j = 0; j < i; j++) {
        lines.add(String.format("  requires m%02di;", j));
      }
      lines.add("}");
      lines.add("");
      Files.write(folder.resolve("module-info.java"), lines);
      lines.clear();
      lines.addAll(
          List.of(
              "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
              "<project>",
              "  <modelVersion>4.0.0</modelVersion>",
              "  <parent>",
              "    <groupId>de.sormuras.bach.doc</groupId>",
              "    <artifactId>bach-doc-ninetynine</artifactId>",
              "    <version>99</version>",
              "  </parent>",
              "",
              String.format("  <artifactId>%s</artifactId>", name),
              "",
              "  <dependencies>"));
      for (int j = 0; j < i; j++) {
        lines.add("    <dependency>");
        lines.add("      <groupId>de.sormuras.bach.doc</groupId>");
        lines.add(String.format("      <artifactId>m%02di</artifactId>", j));
        lines.add("      <version>99</version>");
        lines.add("    </dependency>");
      }
      lines.add("  </dependencies>");
      lines.add("</project>");
      Files.write(base.resolve(name + "/pom.xml"), lines);
    }
  }
}
