## Library

This section describes how directory `lib/` is populated by `${MODULE}-${VERSION}.jar` modules.

+++

### Resolve recursively

Or what is flood-fill?

1. Populate `lib/` directory manually.
1. Resolve modules declared via `library.requires` feature.
1. Resolve all missing `requires ${MODULE}` declared in **project**'s modules.
1. Resolve all missing `requires ${MODULE}` declared in **library**'s modules.
1. **Flood-fill** library by repeating previous step until no required module is missing.

+++

#### Manual modules

Download any modular JAR file and drop it into the `lib/` directory of the project.

+++

#### Library requires

Programmatically via: `new Library(List.of("org.junit.jupiter[@5.6.0]"[, ...]), ...)`

Via configuring a comma-separated list of module names: `library.requires=org.junit.jupiter[@5.6.0][, ...]`

+++

### Resolve single `requires ${MODULE}`

Optional dependence, i.e. `requires static ${MODULE}` are not resolved. Use

Algorithm outline: 

1. Pre-defined Links

   - JavaFX
   - JUnit Platform and Jupiter
   - OpenTest4J
   - APIGuardian

1. User-defined *module-name-to-uri* Links

   - `module/<name>@<default version>=https://repo1.maven.org/maven2/.../<artifactId>-${VERSION}.jar`

1. Fall back to `sormuras/modules` based resolution

    1. User-defined "module version" beats `module-version.properties`
    1. User-defined "module group:artifact" over `module-maven.properties`

1. Throw `UnmappedModuleException`
