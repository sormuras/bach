package com.github.sormuras.bach.internal;

import com.github.sormuras.bach.Builder;
import com.github.sormuras.bach.project.ModuleDeclaration;
import com.github.sormuras.bach.project.Project;
import com.github.sormuras.bach.tool.Command;
import java.lang.module.ModuleDescriptor.Requires;
import java.nio.file.Files;
import java.util.TreeSet;

/** An action that generates a {@code pom.xml} file for each unit in the main code space. */
public class MavenConsumerPomFilesGenerator {

  private final Builder builder;
  private final Project project;
  private final String indent;

  public MavenConsumerPomFilesGenerator(Builder builder, String indent) {
    this.builder = builder;
    this.project = builder.project();
    this.indent = indent;
  }

  public String computeMavenGroupId(ModuleDeclaration unit) {
    var group = System.getProperty("bach.project.deploy.maven.group");
    if (group != null) return group;

    var env = System.getenv();
    // https://docs.github.com/en/actions/configuring-and-managing-workflows/using-environment-variables#default-environment-variables
    if ("true".equals(env.get("GITHUB_ACTIONS")))
      return "com.github." + env.get("GITHUB_REPOSITORY").replace('/', '.');

    // https://jitpack.io/docs/BUILDING/#build-environment
    if ("true".equals(env.get("JITPACK"))) return env.get("GROUP") + '.' + env.get("ARTIFACT");

    return unit.name();
  }

  public String computeMavenArtifactId(ModuleDeclaration unit) {
    return unit.name();
  }

  public String computeMavenVersion() {
    return System.getProperty("bach.project.deploy.maven.version", project.version().toString());
  }

  public void execute() {
    for (var unit : project.spaces().main().modules().map().values()) {
      var pom = new Pom(indent);
      generateHeader(pom);
      generateCoordinates(pom, unit);
      generateDependencies(pom, unit);
      generateFooter(pom);

      try {
        var module = unit.name();
        var maven = project.spaces().main().workspace("deploy", "maven");
        Files.createDirectories(maven);

        // deploy/maven/${MODULE}.pom.xml
        var pomFile = maven.resolve(module + ".pom.xml");
        Files.writeString(pomFile, pom.text.toString());

        // deploy/maven/${MODULE}-empty.jar
        // https://central.sonatype.org/pages/requirements.html#supply-javadoc-and-sources
        var emptyJar = maven.resolve(module + "-empty.jar");
        builder.run(
            Command.builder("jar")
                .with("--create")
                .with("--file", emptyJar)
                .with("-C", maven, pomFile.getFileName())
                .build());

        // deploy/maven/${MODULE}.files
        var archive = builder.computeMainJarFileName(unit);
        var file = project.spaces().main().workspace("modules", archive);
        var files =
            Command.builder("mvn")
                .with("-DpomFile=" + pomFile)
                .with("-Dfile=" + file)
                .with("-Dsources=" + emptyJar)
                .with("-Djavadoc=" + emptyJar)
                .build();
        var cliFile = maven.resolve(module + ".files");
        Files.writeString(cliFile, String.join(" ", files.args()));
      } catch (Exception e) {
        throw new RuntimeException("Write Maven-related file failed: " + e, e);
      }
    }
  }

  public void generateHeader(Pom pom) {
    pom.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
    pom.addNewLine().append("<project xmlns=\"http://maven.apache.org/POM/4.0.0\"");
    pom.addNewLine().append("         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
    pom.addNewLine()
        .append("         ")
        .append("xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0")
        .append(" ")
        .append("http://maven.apache.org/xsd/maven-4.0.0.xsd\">");
    pom.depth(+1).addNewLine().add("modelVersion", "4.0.0");
  }

  public void generateCoordinates(Pom pom, ModuleDeclaration unit) {
    pom.append("\n");
    pom.addNewLine().add("groupId", computeMavenGroupId(unit));
    pom.addNewLine().add("artifactId", computeMavenArtifactId(unit));
    pom.addNewLine().add("version", computeMavenVersion());
  }

  public void generateDependencies(Pom pom, ModuleDeclaration unit) {
    var descriptorRequires = unit.reference().descriptor().requires();
    if (descriptorRequires.isEmpty()) return;
    var declared = Modules.declared(project.spaces().finder());
    var external = Modules.declared(project.externals().finder());
    pom.append("\n");
    pom.addNewLine().depth(+1).append("<dependencies>");
    for (var requires : new TreeSet<>(descriptorRequires)) {
      var name = requires.name();
      if (declared.contains(name)) {
        var required = project.spaces().main().modules().map().get(name);
        generateDependencyForDeclaredModule(pom, required);
        continue;
      }
      if (external.contains(name)) {
        generateDependencyForExternalModule(pom, requires);
        continue;
      }
      pom.addNewLine().append("<!-- system module: " + name + " -->");
    }
    pom.depth(-1).addNewLine().append("</dependencies>");
  }

  public void generateDependencyForExternalModule(Pom pom, Requires requires) {
    pom.addNewLine().append("<!-- " + requires + " -->");
  }

  public void generateDependencyForDeclaredModule(Pom pom, ModuleDeclaration required) {
    pom.addNewLine().depth(+1).append("<dependency>");
    pom.addNewLine().add("groupId", computeMavenGroupId(required));
    pom.addNewLine().add("artifactId", computeMavenArtifactId(required));
    pom.addNewLine().add("version", project.version());
    pom.depth(-1).addNewLine().append("</dependency>");
  }

  public void generateFooter(Pom pom) {
    pom.depth(-1).addNewLine().append("</project>").addNewLine();
  }

  /** An extensible and mutable code builder. */
  public static final class Pom {

    private final StringBuilder text = new StringBuilder();
    private int depth = 0;
    private final String indent;

    public Pom(String indent) {
      this.indent = indent;
    }

    public Pom append(CharSequence sequence) {
      text.append(sequence);
      return this;
    }

    public Pom addNewLine() {
      return append("\n").append(indent.repeat(depth));
    }

    public Pom depth(int delta) {
      this.depth += delta;
      return this;
    }

    public Pom add(String name, Object value) {
      return append("<" + name + ">").append(String.valueOf(value)).append("</" + name + ">");
    }
  }
}
