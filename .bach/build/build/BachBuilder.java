package build;

import static com.github.sormuras.bach.project.ExternalModule.uri;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Builder;
import com.github.sormuras.bach.project.ModuleLookup;
import com.github.sormuras.bach.project.Project;
import com.github.sormuras.bach.tool.ToolCall;
import java.util.Optional;

class BachBuilder extends Builder implements ModuleLookup {

  public BachBuilder(Bach bach, Project project) {
    super(bach, project);
  }

  @Override
  public Optional<String> lookup(String module) {
    var uri =
        switch (module) {
          case "org.apiguardian.api" -> uri("org.apiguardian:apiguardian-api:1.1.1");
          case "org.opentest4j" -> uri("org.opentest4j:opentest4j:1.2.0");
          default -> null;
        };
    return Optional.ofNullable(uri);
  }

  @Override
  public ModuleLookup computeModuleLookup() {
    return ModuleLookup.compose(
        project().externals(), // configured by ProjectInfo::links
        this,
        new JUnitJupiter("5.7.0"),
        new JUnitPlatform("1.7.0"),
        super.computeModuleLookup() // includes project::externals and "best-effort" lookups
        );
  }

  @Override
  public ToolCall computeMainDocumentationJavadocCall() {
    var title = "🎼 Bach " + project().version();
    return super.computeMainDocumentationJavadocCall().toCommand().toBuilder()
        .with("-doctitle", title)
        .build();
  }
}
