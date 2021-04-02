package test.integration.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Options;
import com.github.sormuras.bach.project.LocalModule;
import com.github.sormuras.bach.project.ModuleInfoReference;
import com.github.sormuras.bach.project.SourceFolders;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ModuleDeclarationTests {

  private static final Bach BACH = new Bach(Options.of());

  private static LocalModule newModuleDeclaration(Path root, Path path) {
    return BACH.computeProjectLocalModule(root, path, false);
  }

  @Test
  void empty(@TempDir Path temp) throws Exception {
    var info = Files.writeString(temp.resolve("module-info.java"), "module empty {}");
    var reference = ModuleInfoReference.of(info);
    var sources = new SourceFolders(List.of());
    var resources = new SourceFolders(List.of());
    var declaration = new LocalModule(temp, reference, sources, resources);

    assertEquals("empty", declaration.name());
    assertEquals(info, declaration.reference().info());
    assertTrue(declaration.sources().list().isEmpty());
    assertTrue(declaration.resources().list().isEmpty());
  }

  @Test
  void checkBachMainJava() {
    var module = "com.github.sormuras.bach";
    var info = Path.of(module, "main", "java", "module-info.java");
    var declaration = newModuleDeclaration(Path.of(""), info);
    assertEquals(Path.of(module), declaration.root());
    assertEquals(module, declaration.name());
    var first = declaration.sources().first();
    assertEquals(info.getParent(), first.path());
    assertTrue(first.isModuleInfoJavaPresent());
    assertTrue(declaration.resources().list().isEmpty());
  }

  @Test
  void checkBachTestJava() {
    var module = "com.github.sormuras.bach";
    var java = Path.of(module, "test", "java");
    var info = Path.of(module, "test", "java-module", "module-info.java");
    var declaration = newModuleDeclaration(Path.of(""), info);
    assertEquals(Path.of(module), declaration.root());
    assertEquals(module, declaration.name());
    var first = declaration.sources().first();
    assertEquals(java, first.path());
    assertFalse(first.isModuleInfoJavaPresent());
    var second = declaration.sources().list().get(1);
    assertEquals(info.getParent(), second.path());
    assertTrue(declaration.resources().list().isEmpty());
  }

  @Test
  void checkTestProjectMultiRelease11() {
    var module = "foo";
    var root = Path.of("test.projects", "MultiRelease-11");
    var info = root.resolve(module + "/main/java/module-info.java");
    var declaration = newModuleDeclaration(root, info);
    assertEquals(root.resolve(module), declaration.root());
    assertEquals(module, declaration.name());

    assertEquals(info.getParent(), declaration.sources().first().path());
    assertEquals(0, declaration.sources().first().release());
    assertEquals(12, declaration.sources().list().get(1).release());
    assertEquals(13, declaration.sources().list().get(2).release());
    assertEquals(14, declaration.sources().list().get(3).release());
    assertEquals(15, declaration.sources().list().get(4).release());
    assertEquals(16, declaration.sources().list().get(5).release());
    assertEquals(6, declaration.sources().list().size());
  }

  @Test
  void checkTestProjectJigsawQuickStartGreetings() {
    var module = "com.greetings";
    var root = Path.of("test.projects", "JigsawQuickStartGreetings");
    var info = root.resolve(module + "/module-info.java");
    var declaration = newModuleDeclaration(root, info);
    assertEquals(root.resolve(module), declaration.root());
    assertEquals(module, declaration.name());
    assertEquals(info.getParent(), declaration.sources().first().path());
    assertTrue(declaration.resources().list().isEmpty());
  }

  @Test
  void checkTestProjectSimplicissimus() {
    var module = "simplicissimus";
    var root = Path.of("test.projects", "Simplicissimus");
    var info = root.resolve("module-info.java");
    var declaration = newModuleDeclaration(root, info);
    assertEquals(root, declaration.root());
    assertEquals(module, declaration.name());
    assertTrue(declaration.sources().list().isEmpty());
    assertTrue(declaration.resources().list().isEmpty());
  }
}
