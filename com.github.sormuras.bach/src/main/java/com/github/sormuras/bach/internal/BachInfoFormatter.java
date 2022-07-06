package com.github.sormuras.bach.internal;

import com.github.sormuras.bach.Configuration;
import com.github.sormuras.bach.Project;
import java.util.ArrayList;

public record BachInfoFormatter(Configuration configuration, Project project) {
  public String formatConfiguration() {
    var flags = configuration.flags();
    var paths = configuration.paths();
    return """
             Configuration
               Flags
                              set = %s
               Paths
                             root = %s
                              out = %s
                 external modules = %s
                   external tools = %s
             """
        .formatted(
            flags, paths.root(), paths.out(), paths.externalModules(), paths.externalTools());
  }

  public String formatProject() {
    var lines = new ArrayList<String>();
    lines.add("Project '%s %s'".formatted(project.name(), project.version().value()));
    lines.add("%20s = %s".formatted("name", project.name()));
    lines.add("%20s = %s".formatted("version", project.version().value()));
    lines.add("%20s = %s".formatted("version.date", project.version().date()));
    lines.add("%20s = %s".formatted("modules #", project.modules().size()));
    for (var space : project.spaces().list()) {
      lines.add("%20s = %s".formatted(space.name() + " modules", space.modules().names()));
    }
    for (var space : project.spaces().list()) {
      if (space.modules().list().isEmpty()) continue;
      var info =
          """
          Project space '%s'
          %20s = %s
          %20s = %d
          %20s = %s
          %20s = %s
          """
              .formatted(
                  space.name(),
                  "name",
                  space.name(),
                  "release",
                  space.release(),
                  "depends on spaces",
                  space.requires(),
                  "required modules",
                  ModulesSupport.required(space.modules().toModuleFinder()));
      lines.add(info.stripTrailing());
    }
    return String.join("\n", lines);
  }
}
