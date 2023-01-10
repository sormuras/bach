package run.bach.conf;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import run.bach.Configurator;
import run.bach.Folders;
import run.bach.Options;
import run.bach.Project;
import run.bach.ProjectInfo;
import run.bach.Setting;
import run.bach.Workbench;
import run.bach.internal.PathSupport;

public class ProjectConfigurator implements Configurator {
  @Override
  public void configure(Workbench bench) {
    var project =
        findProjectInfoAnnotation(bench.getConstant(Setting.class).layer())
            .map(info -> (ProjectFactory) new ProjectFactory.OfProjectInfo(bench, info))
            .orElseGet(() -> new ProjectFactory.OfConventions(bench))
            .createProject();
    bench.put(project);
  }

  Optional<ProjectInfo> findProjectInfoAnnotation(ModuleLayer layer) {
    var annotations =
        layer.modules().stream()
            .filter(module -> module.isAnnotationPresent(ProjectInfo.class))
            .map(module -> module.getAnnotation(ProjectInfo.class))
            .toList();
    if (annotations.isEmpty()) return Optional.empty();
    if (annotations.size() > 1) throw new AssertionError("Too many @ProjectInfo found");
    return Optional.of(annotations.get(0));
  }

  @FunctionalInterface
  interface ProjectFactory {
    Workbench bench();

    default Project createProject() {
      var name = createProjectName();
      var version = createProjectVersion();
      var spaces = createProjectSpaces();
      var externals = createProjectExternals();
      return new Project(name, version, spaces, externals);
    }

    default Project.Name createProjectName() {
      return bench()
          .getConstant(Options.class)
          .__project_name()
          .map(Project.Name::new)
          .orElseGet(
              () ->
                  new Project.Name(
                      PathSupport.name(bench().getConstant(Folders.class).root(), "unnamed")));
    }

    default Project.Version createProjectVersion() {
      return new Project.Version("0-ea");
    }

    default Project.Spaces createProjectSpaces() {
      return new Project.Spaces();
    }

    default Project.Externals createProjectExternals() {
      return new Project.Externals();
    }

    record OfConventions(Workbench bench) implements ProjectFactory {
      @Override
      public Project.Spaces createProjectSpaces() {
        return ProjectFactory.super.createProjectSpaces();
      }
    }

    record OfProjectInfo(Workbench bench, ProjectInfo annotation) implements ProjectFactory {
      @Override
      public Project createProject() {
        var name = createProjectName();
        var version = createProjectVersion();
        var spaces = createProjectSpaces();
        var externals = new Project.Externals();
        return new Project(name, version, spaces, externals);
      }

      @Override
      public Project.Name createProjectName() {
        var name = bench().getConstant(Options.class).projectName(annotation.name());
        if (name.equals("*")) return ProjectFactory.super.createProjectName();
        return new Project.Name(name);
      }

      @Override
      public Project.Version createProjectVersion() {
        var version = bench().getConstant(Options.class).projectVersion(annotation.version());
        if (version.equals("*") || version.equalsIgnoreCase("now")) {
          var now = ZonedDateTime.now();
          var year = now.getYear();
          var month = now.getMonthValue();
          var day = now.getDayOfMonth();
          return new Project.Version(String.format("%4d.%02d.%02d-ea", year, month, day), now);
        }
        return new Project.Version(
            version, bench().getConstant(Options.class).projectVersionTimestampOrNow());
      }

      @Override
      public Project.Spaces createProjectSpaces() {
        var spaces = new ArrayList<Project.Space>();
        for (var space : annotation.spaces()) {
          var modules = new ArrayList<Project.DeclaredModule>();
          for (var module : space.modules()) {
            var root =
                annotation
                    .moduleContentRootPattern()
                    .replace("${space}", space.name())
                    .replace("${module}", module);
            var info =
                annotation
                    .moduleContentInfoPattern()
                    .replace("${space}", space.name())
                    .replace("${module}", module);
            var content = bench().getConstant(Folders.class).root(root);
            modules.add(new Project.DeclaredModule(content, content.resolve(info)));
          }
          spaces.add(
              new Project.Space(
                  space.name(),
                  List.of(space.requires()),
                  space.release(),
                  List.of(space.launchers()),
                  new Project.DeclaredModules(modules)));
        }
        return new Project.Spaces(List.copyOf(spaces));
      }
    }
  }
}
