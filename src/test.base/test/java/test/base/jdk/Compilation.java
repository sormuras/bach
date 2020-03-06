/*
 * Bach - Java Shell Builder
 * Copyright (C) 2020 Christian Stein
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package test.base.jdk;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureClassLoader;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import javax.annotation.processing.Processor;
import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

/** In-memory file manager and compiler support. */
public interface Compilation {

  static Class<?> compile(List<String> lines) {
    return compile(String.join(System.lineSeparator(), lines));
  }

  /** Compile Java source, guess its type name return it as a Class instance. */
  static Class<?> compile(String charContent) {
    var packagePattern = Pattern.compile("package\\s+([\\w.]+);");
    var packageMatcher = packagePattern.matcher(charContent);
    var packageName = packageMatcher.find() ? packageMatcher.group(1) + "." : "";
    var namePattern = Pattern.compile("(class|interface|enum)\\s+(.+)\\s*\\{.*");
    var nameMatcher = namePattern.matcher(charContent);
    if (!nameMatcher.find())
      throw new IllegalArgumentException("Expected java compilation unit, but got: " + charContent);
    var className = nameMatcher.group(2).trim();
    return compile(packageName + className, charContent);
  }

  /** Compile Java source for the given class name, load and return it as a Class instance. */
  static Class<?> compile(String className, String charContent) {
    var loader = compile(source(className.replace('.', '/') + ".java", charContent));
    try {
      return loader.loadClass(className);
    } catch (ClassNotFoundException exception) {
      throw new RuntimeException("Class or interface '" + className + "' not found?!", exception);
    }
  }

  static ClassLoader compile(JavaFileObject... units) {
    return compile(null, List.of(), List.of(), List.of(units));
  }

  static ClassLoader compile(
      ClassLoader parent,
      List<String> options,
      List<Processor> processors,
      List<JavaFileObject> units) {
    var compiler = ToolProvider.getSystemJavaCompiler();
    var diagnostics = new DiagnosticCollector<JavaFileObject>();
    var standardFileManager =
        compiler.getStandardFileManager(diagnostics, Locale.getDefault(), StandardCharsets.UTF_8);
    var manager = new Manager(standardFileManager, parent);
    var task = compiler.getTask(null, manager, diagnostics, options, null, units);
    if (!processors.isEmpty()) task.setProcessors(processors);
    boolean success = task.call();
    if (!success) throw new Error("Compilation failed! " + diagnostics.getDiagnostics());
    return manager.getClassLoader(StandardLocation.CLASS_PATH);
  }

  static JavaFileObject source(Path path) throws Exception {
    return source(path.toUri(), Files.readString(path));
  }

  static JavaFileObject source(String uri, String charContent) {
    return source(URI.create(uri), charContent);
  }

  static JavaFileObject source(URI uri, String charContent) {
    return new CharContentFileObject(uri, charContent);
  }

  class ByteArrayFileObject extends SimpleJavaFileObject {

    private ByteArrayOutputStream stream;

    public ByteArrayFileObject(String canonical, Kind kind) {
      super(URI.create("bach:///" + canonical.replace('.', '/') + kind.extension), kind);
    }

    public byte[] getBytes() {
      return stream.toByteArray();
    }

    @Override
    public OutputStream openOutputStream() {
      this.stream = new ByteArrayOutputStream(2000);
      return stream;
    }
  }

  class CharContentFileObject extends SimpleJavaFileObject {

    private final String charContent;
    private final long lastModified;

    public CharContentFileObject(URI uri, String charContent) {
      super(uri, Kind.SOURCE);
      this.charContent = charContent;
      this.lastModified = System.currentTimeMillis();
    }

    @Override
    public String getCharContent(boolean ignoreEncodingErrors) {
      return charContent;
    }

    @Override
    public long getLastModified() {
      return lastModified;
    }
  }

  class SourceFileObject extends SimpleJavaFileObject {

    private ByteArrayOutputStream stream;

    public SourceFileObject(String canonical, Kind kind) {
      super(URI.create("beethoven:///" + canonical.replace('.', '/') + kind.extension), kind);
    }

    @Override
    public String getCharContent(boolean ignoreEncodingErrors) {
      return stream.toString(StandardCharsets.UTF_8);
    }

    @Override
    public OutputStream openOutputStream() {
      this.stream = new ByteArrayOutputStream(2000);
      return stream;
    }
  }

  class Manager extends ForwardingJavaFileManager<StandardJavaFileManager> {

    private final Map<String, ByteArrayFileObject> map = new HashMap<>();
    private final ClassLoader parent;

    public Manager(StandardJavaFileManager standardManager, ClassLoader parent) {
      super(standardManager);
      this.parent = parent != null ? parent : getClass().getClassLoader();
    }

    @Override
    public ClassLoader getClassLoader(Location location) {
      return new SecureLoader(parent, map);
    }

    @Override
    public JavaFileObject getJavaFileForOutput(
        Location location, String name, JavaFileObject.Kind kind, FileObject sibling) {
      switch (kind) {
        case CLASS:
          ByteArrayFileObject object = new ByteArrayFileObject(name, kind);
          map.put(name, object);
          return object;
        case SOURCE:
          return new SourceFileObject(name, kind);
        default:
          throw new UnsupportedOperationException("kind not supported: " + kind);
      }
    }

    @Override
    public boolean isSameFile(FileObject fileA, FileObject fileB) {
      return fileA.toUri().equals(fileB.toUri());
    }
  }

  class SecureLoader extends SecureClassLoader {
    private final Map<String, ByteArrayFileObject> map;

    public SecureLoader(ClassLoader parent, Map<String, ByteArrayFileObject> map) {
      super("SecureLoader", parent);
      this.map = map;
    }

    @Override
    protected Class<?> findClass(String className) throws ClassNotFoundException {
      try {
        return getParent().loadClass(className);
      } catch (ClassNotFoundException e) {
        // fall-through
      }
      var object = map.get(className);
      if (object == null) {
        throw new ClassNotFoundException(className);
      }
      byte[] bytes = object.getBytes();
      return super.defineClass(className, bytes, 0, bytes.length);
    }
  }
}
