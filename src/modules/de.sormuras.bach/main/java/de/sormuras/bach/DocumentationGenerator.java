package de.sormuras.bach;

import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.TRACE;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.Callable;

/*BODY*/
/** Generate documentation. */
public /*STATIC*/ class DocumentationGenerator implements Callable<Integer> {

  final Bach bach;
  final Project project;

  DocumentationGenerator(Bach bach) {
    this.bach = bach;
    this.project = bach.project;
  }

  @Override
  public Integer call() throws Exception {
    bach.run.log(TRACE, "DocumentationGenerator::call()");
    document("main");
    return 0;
  }

  private void document(String realm) {
    var modules = bach.project.modules(realm);
    if (modules.isEmpty()) {
      bach.run.log(DEBUG, "No %s modules found.", realm);
      return;
    }
    var documentation = project.bin.resolve(realm).resolve("documentation");
    var destination = documentation.resolve("javadoc");
    var moduleSourcePath = project.src + "/*/" + realm + "/java";
    var javaSources = new ArrayList<String>();
    javaSources.add(moduleSourcePath);
    for (var release = 7; release <= Runtime.version().feature(); release++) {
      var separator = File.separator;
      javaSources.add(project.src + separator + "*" + separator + "java-" + release);
    }
    bach.run.run(
        new Command("javadoc")
            .addIff(false, "-verbose")
            .add("-encoding", "UTF-8")
            .add("-quiet")
            .add("-windowtitle", project.name)
            .add("-d", destination)
            .add("--module-source-path", String.join(File.pathSeparator, javaSources))
            .add("--module", String.join(",", modules)));

    //    var modulePath = realm.modulePath("compile");
    //    if (!modulePath.isEmpty()) {
    //      javadoc.add("--module-path", modulePath);
    //    }

    var javadocJar = documentation.resolve(project.name + "-" + project.version + "-javadoc.jar");
    bach.run.run(
        new Command("jar")
            .add("--create")
            .addIff(false, "--verbose")
            .add("--file", javadocJar)
            .add("-C", destination)
            .add("."));
  }
}
