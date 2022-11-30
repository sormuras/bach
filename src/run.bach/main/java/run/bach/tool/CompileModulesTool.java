package run.bach.tool;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import run.bach.Folders;
import run.bach.Project;
import run.bach.ProjectOperator;
import run.bach.Workbench;
import run.duke.ToolCall;

public class CompileModulesTool extends ProjectOperator {
  public static final String NAME = "compile-modules";

  public CompileModulesTool(Project project, Workbench workbench) {
    super(NAME, project, workbench);
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    var calls = new TreeMap<String, List<ToolCall>>();
    var space = project().spaces().space(args[0]);
    for (var module : space.modules()) {
      var context = new OperationContext(folders(), space, module, calls);
      var jar = createJarCall();
      jar = jar.with("--create");
      jar = jarWithFile(jar, context);
      jar = jarWithModuleVersion(jar, context);
      jar = jarWithDate(jar, context);
      jar = jarWithLauncher(jar, context);
      jar = jarWithBaseClassesAndResources(jar, context);
      jar = jarWithClassesOfPatchedModule(jar, context); // instead of "--patch-module" at runtime
      jar = jarWithTargetedClassesAndResources(jar, context);
      context.withJarCall(jar);
    }
    var modules = folders().out(space.name(), "modules");
    if (Runtime.version().feature() < 19) {
      try {
        Files.createDirectories(modules); // ToolCall.of("tree").with("create").with(modules);
      } catch (Exception exception) {
        throw new RuntimeException("Create directories failed: " + modules);
      }
    }
    for (var list : calls.values()) {
      list.stream().parallel().forEach(this::run);
    }
    // TODO bach.run(HashTool.class, modules.toString());
    return 0;
  }

  protected ToolCall createJarCall() {
    return ToolCall.of("jar");
  }

  protected ToolCall jarWithFile(ToolCall jar, OperationContext context) {
    var file = context.modules().resolve(context.module().name() + ".jar");
    return jar.with("--file", file);
  }

  protected ToolCall jarWithModuleVersion(ToolCall jar, OperationContext context) {
    return jar.with("--module-version", project().version().value());
  }

  protected ToolCall jarWithDate(ToolCall jar, OperationContext context) {
    if (Runtime.version().feature() < 19) return jar;
    var timestamp = project().version().timestamp().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    return jar.with("--date", timestamp);
  }

  protected ToolCall jarWithLauncher(ToolCall jar, OperationContext context) {
    var space = context.space();
    if (space.launcher().isEmpty()) return jar;
    var launcher = space.launcher().get();
    var name = context.module().name();
    if (!launcher.startsWith(name + '/')) return jar;
    var className = launcher.substring(name.length() + 1);
    return jar.with("--main-class", className);
  }

  protected ToolCall jarWithBaseClassesAndResources(ToolCall jar, OperationContext context) {
    var release0 = context.space().targets();
    var feature0 = Runtime.version().feature();
    var classes0 = context.classes().resolve("java-" + release0.orElse(feature0));
    var name = context.module().name();
    if (Files.isDirectory(classes0.resolve(name))) {
      jar = jar.with("-C", classes0.resolve(name), ".");
    }
    for (var resources : context.module().base().resources()) {
      jar = jar.with("-C", resources, ".");
    }
    return jar;
  }

  protected ToolCall jarWithClassesOfPatchedModule(ToolCall jar, OperationContext context) {
    var name = context.module().name();
    for (var requires : context.space().requires()) {
      var required = project().spaces().space(requires);
      if (required.modules().find(name).isPresent()) {
        var javaR = "java-" + required.targets().orElse(Runtime.version().feature());
        jar = jar.with("-C", folders().out(requires, "classes", javaR, name), ".");
      }
    }
    return jar;
  }

  protected ToolCall jarWithTargetedClassesAndResources(ToolCall jar, OperationContext context) {
    var module = context.module();
    var name = context.module().name();
    var release0 = context.space().targets();
    var feature0 = Runtime.version().feature();
    var classes0 = context.classes().resolve("java-" + release0.orElse(feature0));
    for (var release : module.targeted().keySet().stream().sorted().toList()) {
      var folders = module.targeted().get(release);
      for (var sources : folders.sources()) {
        var classesR = context.classes().resolve("java-" + release).resolve(name);
        var javac = ToolCall.of("javac").with("--release", release);
        var modulePath = context.space().toModulePath(folders());
        if (modulePath.isPresent()) {
          javac = javac.with("--module-path", modulePath.get());
          javac = javac.with("--processor-module-path", modulePath.get());
        }
        javac =
            javac
                .with("--class-path", classes0.resolve(name))
                .with("-implicit:none")
                .with("-d", classesR)
                .withFindFiles(sources, "**.java");
        context.withJavacCall(javac);
        jar = jar.with("--release", release).with("-C", classesR, ".");
      }
      var needsReleaseArgument = folders.sources().isEmpty() && !folders.resources().isEmpty();
      if (needsReleaseArgument) jar = jar.with("--release", release);
      for (var resources : folders.resources()) {
        jar = jar.with("-C", resources, ".");
      }
    }
    return jar;
  }

  public record OperationContext(
      Folders paths,
      Project.Space space,
      Project.DeclaredModule module,
      Map<String, List<ToolCall>> calls) {
    public Path classes() {
      return paths.out(space().name(), "classes");
    }

    public Path modules() {
      return paths.out(space().name(), "modules");
    }

    public void withJavacCall(ToolCall javac) {
      calls.computeIfAbsent("10 javac calls", __ -> new ArrayList<>()).add(javac);
    }

    public void withJarCall(ToolCall jar) {
      calls.computeIfAbsent("20 jar calls", __ -> new ArrayList<>()).add(jar);
    }
  }
}
