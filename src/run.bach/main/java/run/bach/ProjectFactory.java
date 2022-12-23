package run.bach;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import run.bach.internal.PathSupport;

@FunctionalInterface
public interface ProjectFactory {
  Composer composer();

  static Project createProjectFromAnnotationAttachedToComposersModule(Composer composer) {
    var annotation = composer.getClass().getModule().getAnnotation(ProjectInfo.class);
    return new OfProjectInfo(composer, annotation).createProject();
  }

  default Project createProject() {
    var name = createProjectName();
    var version = createProjectVersion();
    var spaces = createProjectSpaces();
    var externals = createProjectExternals();
    return new Project(name, version, spaces, externals);
  }

  default Project.Name createProjectName() {
    return options()
        .__project_name()
        .map(Project.Name::new)
        .orElseGet(() -> new Project.Name(PathSupport.name(folders().root(), "unnamed")));
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

  default Options options() {
    return composer().options;
  }

  default Folders folders() {
    return composer().folders;
  }

  record OfConventions(Composer composer) implements ProjectFactory {
    @Override
    public Project.Spaces createProjectSpaces() {
      return ProjectFactory.super.createProjectSpaces();
    }
  }

  record OfProjectInfo(Composer composer, ProjectInfo annotation) implements ProjectFactory {
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
      var name = options().projectName(annotation.name());
      if (name.equals("*")) return ProjectFactory.super.createProjectName();
      return new Project.Name(name);
    }

    @Override
    public Project.Version createProjectVersion() {
      var version = options().projectVersion(annotation.version());
      if (version.equals("*") || version.equalsIgnoreCase("now")) {
        var now = ZonedDateTime.now();
        var year = now.getYear();
        var month = now.getMonthValue();
        var day = now.getDayOfMonth();
        return new Project.Version(String.format("%4d.%02d.%02d-ea", year, month, day), now);
      }
      return new Project.Version(version, options().projectVersionTimestampOrNow());
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
          var content = folders().root(root);
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
