package test.base.magnificat;

import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.spi.ToolProvider;
import test.base.magnificat.api.Option;
import test.base.magnificat.api.ProjectInfo;
import test.base.magnificat.internal.ModuleLayerBuilder;

record Configurator(Printer printer) {
  Configuration configure(Options cliOptions) {
    var defaultOptions = Options.ofAllDefaults();
    var module = newModule(Options.compose(cliOptions, defaultOptions));
    var info = getProjectInfo(module);

    var options =
        Options.compose(
            // --project-name A
            cliOptions,
            // arguments = {"--project-name", "B"}
            Options.ofCommandLineArguments(info.arguments()),
            // option = PROJECT_NAME, value = "C"
            Options.ofProjectInfoOptions(info.options()),
            // project.name = "D" (defaults "E")
            Options.ofProjectInfoElements(info),
            // PROJECT_NAME.defaults() ("E")
            defaultOptions);

    var logbook = newLogbook(options);
    var binding = newBinding(module.getLayer());
    return new Configuration(logbook, binding, options);
  }

  Module newModule(Options options) {
    var root = Path.of(options.get(Option.CLI_BACH_ROOT)).normalize();
    var infoFolder = root.resolve(options.get(Option.CLI_BACH_INFO_FOLDER)).normalize();
    var infoModule = options.get(Option.CLI_BACH_INFO_MODULE);
    var layer = newModuleLayer(infoModule, infoFolder);
    return layer.findModule(infoModule).orElse(Bach.class.getModule());
  }

  ModuleLayer newModuleLayer(String module, Path source) {
    var moduleInfo = source.resolve(module).resolve("module-info.java");
    if (Files.notExists(moduleInfo)) return ModuleLayer.empty();

    var bach = Bach.location();
    var directory = String.format("bach-%s-%08x", module, new Random().nextInt());
    var destination = Path.of(System.getProperty("java.io.tmpdir"), directory);
    var args =
        List.of(
            "--module",
            module,
            "--module-source-path",
            source.toString(),
            "--module-path",
            bach.toString(),
            "-encoding",
            "UTF-8",
            "-d",
            destination.toString());
    var javac = ToolProvider.findFirst("javac").orElseThrow();
    var result = javac.run(printer.out(), printer.err(), args.toArray(String[]::new));
    if (result != 0) throw new RuntimeException("Non-zero exit code: " + result + " with: " + args);

    var boot = ModuleLayer.boot();
    return new ModuleLayerBuilder()
        .bindServices(true)
        .oneLoader(true)
        .parentConfigurations(List.of(boot.configuration()))
        .before(ModuleFinder.of())
        .after(ModuleFinder.of(destination))
        .roots(Set.of(module))
        .parentLayers(List.of(boot))
        .parentLoader(ClassLoader.getPlatformClassLoader())
        .controllerConsumer(controller -> {})
        .build();
  }

  ProjectInfo getProjectInfo(Module module) {
    var info = module.getAnnotation(ProjectInfo.class);
    if (info != null) return info;
    return ProjectInfo.class.getModule().getAnnotation(ProjectInfo.class);
  }

  Logbook newLogbook(Options options) {
    return Logbook.of(printer, options.is(Option.VERBOSE));
  }

  Binding newBinding(ModuleLayer layer) {
    return ServiceLoader.load(layer, Binding.class).findFirst().orElseGet(Binding::new);
  }
}
