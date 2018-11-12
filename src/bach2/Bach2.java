import static java.lang.System.Logger.Level.DEBUG;
import static java.util.Objects.requireNonNull;

import java.io.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.net.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.spi.*;
import java.util.stream.*;

interface Bach2 {

  interface Shadow {
    // /bartholdy-0.2.1/src/main/java/de/sormuras/bartholdy/Bartholdy.java

    final class Bartholdy {

      private static final System.Logger LOG = System.getLogger(Bartholdy.class.getName());

      public static void main(String[] args) {
        System.out.println("Bartholdy " + version());
      }

      public static Path currentJdkHome() {
        var executable = ProcessHandle.current().info().command().map(Path::of).orElseThrow();
        return executable.getParent().getParent().toAbsolutePath();
      }

      static String fileName(URI uri) {
        var urlString = uri.getPath();
        var begin = urlString.lastIndexOf('/') + 1;
        return urlString.substring(begin).split("\\?")[0].split("#")[0];
      }

      public static Path download(URI uri, Path tools) {
        return download(uri, fileName(uri), tools);
      }

      public static Path download(URI uri, String fileName, Path tools) {
        var localPath = tools.resolve(fileName);
        if (Files.exists(localPath)) {
          return localPath;
        }
        try {
          var rbc = Channels.newChannel(uri.toURL().openStream());
          Files.createDirectories(tools);
          var fos = new FileOutputStream(localPath.toFile());
          fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
          return localPath;
        } catch (IOException e) {
          throw new UncheckedIOException("download failed", e);
        }
      }

      public static Path install(URI uri, Path tools) {
        return install(uri, fileName(uri), tools);
      }

      public static Path install(URI uri, String zip, Path tools) {
        var localZip = download(uri, zip, tools);
        try {
          var jarTool = ToolProvider.findFirst("jar").orElseThrow();
          var listing = new StringWriter();
          var printWriter = new PrintWriter(listing);
          jarTool.run(printWriter, printWriter, "--list", "--file", localZip.toString());
          var root = Path.of(listing.toString().split("\\R")[0]);
          var home = tools.resolve(root);
          if (Files.notExists(home)) {
            jarTool.run(System.out, System.err, "--extract", "--file", localZip.toString());
            Files.move(root, home);
          }
          return home.normalize().toAbsolutePath();
        } catch (IOException e) {
          throw new UncheckedIOException("install failed", e);
        }
      }

      public static String read(Path jar, String entry, String delimiter, String defaultValue) {
        try (var fs = FileSystems.newFileSystem(jar, null)) {
          for (var root : fs.getRootDirectories()) {
            var versionPath = root.resolve(entry);
            if (Files.exists(versionPath)) {
              return String.join(delimiter, Files.readAllLines(versionPath));
            }
          }
        } catch (IOException e) {
          throw new UncheckedIOException("read entry failed", e);
        }
        return defaultValue;
      }

      public static String readProperty(String source, String key, String defaultValue) {
        var properties = new Properties();
        try {
          properties.load(new StringReader(source));
        } catch (IOException e) {
          throw new UncheckedIOException("read property failed", e);
        }
        return properties.getProperty(key, defaultValue);
      }

      public static Path setExecutable(Path path) {
        if (Files.isExecutable(path)) {
          return path;
        }
        if (!FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
          LOG.log(System.Logger.Level.DEBUG, "default file system doesn't support posix");
          return path;
        }
        var program = path.toFile();
        var ok = program.setExecutable(true);
        if (!ok) {
          LOG.log(System.Logger.Level.WARNING, "couldn't set executable flag: " + program);
        }
        return path;
      }

      public static String version() {
        var loader = Bartholdy.class.getClassLoader();
        try (var is = loader.getResourceAsStream("de/sormuras/bartholdy/version.properties")) {
          if (is == null) {
            return "DEVELOPMENT";
          }
          var properties = new Properties();
          properties.load(is);
          return properties.getProperty("version", "UNKNOWN");
        } catch (IOException e) {
          throw new UncheckedIOException("read version failed", e);
        }
      }

      private Bartholdy() {
        throw new UnsupportedOperationException();
      }
    }
    // /bartholdy-0.2.1/src/main/java/de/sormuras/bartholdy/Configuration.java

    interface Configuration {

      static Builder builder() {
        return new Builder();
      }

      static Configuration of(Object... args) {
        return builder().setArguments(args).build();
      }

      List<String> getArguments();

      Map<String, String> getEnvironment();

      Path getTemporaryDirectory();

      Path getWorkingDirectory();

      Duration getTimeout();

      default Builder toBuilder() {
        return builder()
            .setArguments(new ArrayList<>(getArguments()))
            .setEnvironment(new HashMap<>(getEnvironment()));
      }

      class Builder implements Configuration {

        private final System.Logger logger = System.getLogger(getClass().getCanonicalName());
        private boolean mutable = true;
        private List<String> arguments = new ArrayList<>();
        private Map<String, String> environment = new HashMap<>();
        private Path temporaryDirectory = Path.of(System.getProperty("java.io.tmpdir"));
        private Path workingDirectory = Path.of(".").normalize().toAbsolutePath();
        private Duration timeout = Duration.ofSeconds(9);

        public Configuration build() {
          mutable = false;
          arguments = List.copyOf(arguments);
          environment = Map.copyOf(environment);
          return this;
        }

        @Override
        public String toString() {
          return "Configuration{"
              + "arguments="
              + arguments
              + ", timeout="
              + timeout
              + ", environment="
              + environment
              + ", temporaryDirectory="
              + temporaryDirectory
              + ", workingDirectory="
              + workingDirectory
              + '}';
        }

        void checkMutableState() {
          if (isMutable()) {
            return;
          }
          throw new IllegalStateException("immutable");
        }

        public boolean isMutable() {
          return mutable;
        }

        @Override
        public List<String> getArguments() {
          return arguments;
        }

        public Builder addArgument(Object argument) {
          checkMutableState();
          this.arguments.add(String.valueOf(requireNonNull(argument, "argument must not be null")));
          return this;
        }

        public Builder setArguments(List<String> arguments) {
          checkMutableState();
          this.arguments = requireNonNull(arguments, "arguments must not be null");
          return this;
        }

        public Builder setArguments(Object... arguments) {
          checkMutableState();
          this.arguments.clear();
          for (var argument : arguments) {
            if (argument instanceof Iterable) {
              logger.log(DEBUG, "unrolling iterable argument: " + argument);
              ((Iterable<?>) argument).forEach(this::addArgument);
              continue;
            }
            addArgument(argument);
          }
          return this;
        }

        @Override
        public Map<String, String> getEnvironment() {
          return environment;
        }

        Builder setEnvironment(Map<String, String> environment) {
          checkMutableState();
          this.environment = environment;
          return this;
        }

        public Builder putEnvironment(String key, String value) {
          checkMutableState();
          requireNonNull(key, "key must not be null");
          requireNonNull(value, "value must not be null");
          environment.put(key, value);
          return this;
        }

        @Override
        public Path getTemporaryDirectory() {
          return temporaryDirectory;
        }

        public Builder setTemporaryDirectory(Path temporaryDirectory) {
          checkMutableState();
          requireNonNull(temporaryDirectory, "temporaryDirectory must not be null");
          this.temporaryDirectory = temporaryDirectory;
          return this;
        }

        @Override
        public Path getWorkingDirectory() {
          return workingDirectory;
        }

        public Builder setWorkingDirectory(Path workingDirectory) {
          checkMutableState();
          requireNonNull(workingDirectory, "workingDirectory must not be null");
          this.workingDirectory = workingDirectory;
          return this;
        }

        @Override
        public Duration getTimeout() {
          return timeout;
        }

        public Builder setTimeoutMillis(long timeoutMillis) {
          setTimeout(Duration.ofMillis(timeoutMillis));
          return this;
        }

        public Builder setTimeout(Duration timeout) {
          checkMutableState();
          requireNonNull(timeout, "timeout must not be null");
          this.timeout = timeout;
          return this;
        }
      }
    }
    // /bartholdy-0.2.1/src/main/java/de/sormuras/bartholdy/Reflector.java

    class Reflector {

      @Retention(RetentionPolicy.RUNTIME)
      @Target(ElementType.FIELD)
      public @interface Option {
        String value();
      }

      public static List<String> reflect(Object options) {
        var arguments = new ArrayList<String>();
        new Reflector(options, arguments::add).reflect();
        return List.copyOf(arguments);
      }

      private final Object options;
      private final UnaryOperator<Stream<Field>> operator;
      private final Consumer<String> consumer;

      public Reflector(Object options, Consumer<String> consumer) {
        this(options, UnaryOperator.identity(), consumer);
      }

      public Reflector(
          Object options, UnaryOperator<Stream<Field>> operator, Consumer<String> consumer) {
        this.options = options;
        this.operator = operator;
        this.consumer = consumer;
      }

      public Reflector add(Object argument) {
        consumer.accept(argument.toString());
        return this;
      }

      Reflector add(Collection<Path> paths) {
        return add(paths.stream(), File.pathSeparator);
      }

      Reflector add(Stream<?> stream, String separator) {
        return add(stream.map(Object::toString).collect(Collectors.joining(separator)));
      }

      public void reflect() {
        var stream =
            Arrays.stream(options.getClass().getDeclaredFields())
                .filter(field -> !field.isSynthetic())
                .filter(field -> !Modifier.isStatic(field.getModifiers()))
                .filter(field -> !Modifier.isPrivate(field.getModifiers()))
                .filter(field -> !Modifier.isTransient(field.getModifiers()));
        stream = operator.apply(stream);
        stream.forEach(this::reflectField);
      }

      private void reflectField(Field field) {
        try {
          reflectFieldThrowing(field);
        } catch (ReflectiveOperationException e) {
          throw new Error("reflecting field '" + field + "' failed for " + options, e);
        }
      }

      private void reflectFieldThrowing(Field field) throws ReflectiveOperationException {

        try {
          options
              .getClass()
              .getDeclaredMethod(field.getName(), Reflector.class)
              .invoke(options, this);
          return;
        } catch (NoSuchMethodException e) {
        }

        var value = field.get(options);
        if (value == null) {
          return;
        }
        if (value instanceof Collection && ((Collection) value).isEmpty()) {
          return;
        }

        var optional = Optional.ofNullable(field.getAnnotation(Option.class));
        var optionName = optional.map(Option::value).orElse(getOptionName(field.getName()));

        if (field.getType() == boolean.class) {
          if (field.getBoolean(options)) {
            add(optionName);
          }
          return;
        }
        if (!optionName.isEmpty()) {
          add(optionName);
        }
        if (value instanceof Collection) {
          var iterator = ((Collection) value).iterator();
          var head = iterator.next();

          if (head instanceof Path) {
            @SuppressWarnings("unchecked")
            var paths = (Collection<Path>) value;
            add(paths);
            return;
          }
        }
        add(value.toString());
      }

      private String getOptionName(String fieldName) {
        var hasUppercase = !fieldName.equals(fieldName.toLowerCase());
        var defaultName = new StringBuilder();
        if (hasUppercase) {
          defaultName.append("--");
          fieldName
              .chars()
              .forEach(
                  i -> {
                    if (Character.isUpperCase(i)) {
                      defaultName.append('-');
                      defaultName.append((char) Character.toLowerCase(i));
                    } else {
                      defaultName.append((char) i);
                    }
                  });
        } else {
          defaultName.append('-');
          defaultName.append(fieldName.replace('_', '-'));
        }
        return defaultName.toString();
      }
    }
    // /bartholdy-0.2.1/src/main/java/de/sormuras/bartholdy/Result.java

    interface Result {

      static Builder builder() {
        return new Builder();
      }

      int getExitCode();

      Duration getDuration();

      default String getOutput(String key) {
        return String.join("\n", getOutputLines(key));
      }

      List<String> getOutputLines(String key);

      boolean isTimedOut();

      class Builder implements Result {

        private int exitCode = Integer.MIN_VALUE;
        private Duration duration = Duration.ZERO;
        private Map<String, List<String>> lines = new HashMap<>();
        private boolean timedOut;

        public Result build() {
          requireNonNull(duration, "duration must not be null");
          return this;
        }

        @Override
        public String toString() {
          return "Result{"
              + "exitCode="
              + exitCode
              + ", timedOut="
              + timedOut
              + ", duration="
              + duration
              + ", lines="
              + lines
              + '}';
        }

        @Override
        public int getExitCode() {
          return exitCode;
        }

        public Builder setExitCode(int exitCode) {
          this.exitCode = exitCode;
          return this;
        }

        @Override
        public Duration getDuration() {
          return duration;
        }

        public Builder setDuration(Duration duration) {
          this.duration = duration;
          return this;
        }

        public Builder setOutput(String key, String output) {
          return setOutput(key, List.of(output.split("\\R")));
        }

        public Builder setOutput(String key, List<String> output) {
          this.lines.put(key, output);
          return this;
        }

        @Override
        public List<String> getOutputLines(String key) {
          return lines.getOrDefault(key, List.of());
        }

        @Override
        public boolean isTimedOut() {
          return timedOut;
        }

        public Builder setTimedOut(boolean timedOut) {
          this.timedOut = timedOut;
          return this;
        }
      }
    }
    // /bartholdy-0.2.1/src/main/java/de/sormuras/bartholdy/Tool.java

    interface Tool {

      String getName();

      default String getProgram() {
        return getName();
      }

      String getVersion();

      default int run(Object... args) {
        Objects.requireNonNull(args, "args must not be null");
        return run(Configuration.of(args)).getExitCode();
      }

      Result run(Configuration configuration);
    }
    // /bartholdy-0.2.1/src/main/java/de/sormuras/bartholdy/jdk/AbstractJdkTool.java

    abstract class AbstractJdkTool implements Tool {

      private final System.Logger logger;
      private final String name;

      AbstractJdkTool() {
        this.name = getClass().getSimpleName().toLowerCase();
        this.logger = System.getLogger(name);
      }

      @Override
      public String getName() {
        return name;
      }

      @Override
      public String getVersion() {
        return Runtime.version().toString();
      }

      @Override
      public Result run(Configuration configuration) {
        logger.log(DEBUG, "Running...");
        var provider = ToolProvider.findFirst(getName()).orElseThrow();
        logger.log(DEBUG, "Found %s", provider);
        var start = Instant.now();
        var out = new StringWriter();
        var err = new StringWriter();
        var args = configuration.getArguments().toArray(new String[0]);
        var code = provider.run(new PrintWriter(out), new PrintWriter(err), args);
        var duration = Duration.between(start, Instant.now());
        logger.log(DEBUG, "Took %s", duration);
        return Result.builder()
            .setExitCode(code)
            .setDuration(duration)
            .setOutput("out", out.toString())
            .setOutput("err", err.toString())
            .build();
      }
    }
    // /bartholdy-0.2.1/src/main/java/de/sormuras/bartholdy/jdk/Jar.java

    class Jar extends AbstractJdkTool {}
    // /bartholdy-0.2.1/src/main/java/de/sormuras/bartholdy/jdk/Javac.java

    class Javac extends AbstractJdkTool {}
    // /bartholdy-0.2.1/src/main/java/de/sormuras/bartholdy/jdk/Javadoc.java

    class Javadoc extends AbstractJdkTool {}
    // /bartholdy-0.2.1/src/main/java/de/sormuras/bartholdy/jdk/Javap.java

    class Javap extends AbstractJdkTool {}
    // /bartholdy-0.2.1/src/main/java/de/sormuras/bartholdy/jdk/Jdeps.java

    class Jdeps extends AbstractJdkTool {}
    // /bartholdy-0.2.1/src/main/java/de/sormuras/bartholdy/jdk/Jlink.java

    class Jlink extends AbstractJdkTool {}
    // /bartholdy-0.2.1/src/main/java/de/sormuras/bartholdy/jdk/Jmod.java

    class Jmod extends AbstractJdkTool {}
    // /bartholdy-0.2.1/src/main/java/de/sormuras/bartholdy/tool/AbstractTool.java

    abstract class AbstractTool implements Tool {

      @Override
      public Result run(Configuration configuration) {
        var timeout = configuration.getTimeout().toMillis();
        var command = createCommand(configuration);
        var builder = new ProcessBuilder(command);
        builder.directory(configuration.getWorkingDirectory().toFile());
        builder.environment().put("JAVA_HOME", Bartholdy.currentJdkHome().toString());
        builder.environment().put(getNameOfEnvironmentHomeVariable(), getHome().toString());
        builder.environment().putAll(configuration.getEnvironment());
        try {
          var errfile = Files.createTempFile("bartholdy-err-", ".txt");
          var outfile = Files.createTempFile("bartholdy-out-", ".txt");
          builder.redirectError(errfile.toFile());
          builder.redirectOutput(outfile.toFile());
          var start = Instant.now();
          var process = builder.start();
          try {
            var timedOut = false;
            if (!process.waitFor(timeout, TimeUnit.MILLISECONDS)) {
              timedOut = true;
              process.destroy();
            }
            var duration = Duration.between(start, Instant.now());
            return Result.builder()
                .setTimedOut(timedOut)
                .setExitCode(process.exitValue())
                .setDuration(duration)
                .setOutput("err", readAllLines(errfile))
                .setOutput("out", readAllLines(outfile))
                .build();
          } catch (InterruptedException e) {
            throw new RuntimeException("run failed", e);
          } finally {

            Files.deleteIfExists(errfile);
            Files.deleteIfExists(outfile);
          }
        } catch (IOException e) {
          throw new UncheckedIOException("starting process failed", e);
        }
      }

      private List<String> createCommand(Configuration configuration) {
        var program = createProgram(createPathToProgram());
        var command = new ArrayList<String>();
        command.add(program);
        command.addAll(getToolArguments());
        command.addAll(configuration.getArguments());
        var commandLineLength = String.join(" ", command).length();
        if (commandLineLength < 32000) {
          return command;
        }
        var timestamp = Instant.now().toString().replace("-", "").replace(":", "");
        var prefix = "bartholdy-" + getName() + "-arguments-" + timestamp + "-";
        try {
          var temporaryDirectory = configuration.getTemporaryDirectory();
          var temporaryFile = Files.createTempFile(temporaryDirectory, prefix, ".txt");
          return List.of(program, "@" + Files.write(temporaryFile, configuration.getArguments()));
        } catch (IOException e) {
          throw new UncheckedIOException("creating temporary arguments file failed", e);
        }
      }

      public Path getHome() {
        return Path.of(".");
      }

      public String getNameOfEnvironmentHomeVariable() {
        return getClass().getSimpleName().toUpperCase() + "_HOME";
      }

      protected Path createPathToProgram() {
        return getHome().resolve("bin").resolve(getProgram());
      }

      protected String createProgram(Path pathToProgram) {
        return pathToProgram.normalize().toAbsolutePath().toString();
      }

      protected List<String> getToolArguments() {
        return List.of();
      }

      private static List<String> readAllLines(Path path) {
        try {
          return Files.readAllLines(path);
        } catch (IOException e) {
        }
        var lines = new ArrayList<String>();
        try (BufferedReader br = new BufferedReader(new FileReader(path.toFile()))) {
          for (String line; (line = br.readLine()) != null; ) {
            lines.add(line);
          }
        } catch (IOException e) {
          throw new UncheckedIOException("reading lines failed: " + path, e);
        }
        return lines;
      }
    }
    // /bartholdy-0.2.1/src/main/java/de/sormuras/bartholdy/tool/Java.java

    class Java extends AbstractTool {

      @Override
      public Path getHome() {
        return Bartholdy.currentJdkHome();
      }

      @Override
      public String getName() {
        return "java";
      }

      @Override
      public final String getProgram() {
        return "java";
      }

      @Override
      public String getVersion() {
        return Runtime.version().toString();
      }
    }
  }
}
