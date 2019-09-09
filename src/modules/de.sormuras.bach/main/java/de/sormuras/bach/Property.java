package de.sormuras.bach;

/*BODY*/
/** Bach's property collection. */
enum Property {
  /** Be verbose. */
  DEBUG("ebug", "false"),

  /** Name of the project. */
  PROJECT_NAME("name", "Project"),
  /** Version of the project (used for every module). */
  PROJECT_VERSION("version", "0"),

  /** Base directory of the project. */
  BASE_DIRECTORY("base", ""),
  /** Directory with 3rd-party modules, relative to {@link #BASE_DIRECTORY}. */
  LIBRARY_DIRECTORY("library", "lib"),
  /** Directory with modules sources, relative to {@link #BASE_DIRECTORY}. */
  SOURCE_DIRECTORY("source", "src"),
  /** Directory that contains generated binary assets, relative to {@link #BASE_DIRECTORY}. */
  TARGET_DIRECTORY("target", "bin"),

  /** Default Maven 2 repository used for resolving missing modules. */
  MAVEN_REPOSITORY("maven.repository", "https://repo1.maven.org/maven2"),

  /** Options passed to all 'javac' calls. */
  TOOL_JAVAC_OPTIONS("tool.javac.options", "-encoding\nUTF-8\n-parameters\n-Xlint"),
  /** Options passed to all 'junit' calls. */
  TOOL_JUNIT_OPTIONS("tool.junit.options", "--fail-if-no-tests\n--details=tree"),
  ;

  private final String key;
  private final String defaultValue;

  Property(String key, String defaultValue) {
    this.key = key;
    this.defaultValue = defaultValue;
  }

  public String getKey() {
    return key;
  }

  String getDefaultValue() {
    return defaultValue;
  }

  String get() {
    return get(defaultValue);
  }

  String get(String defaultValue) {
    return System.getProperty(key, defaultValue);
  }
}
