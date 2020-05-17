## Usage

Two distinct "Run Modes" are available.

### Single-file source-code API

`Bach.java` as `Bach.java`, literally.

#### Zero-installation build via `jshell <load-file>`.

Let `jshell` load, compile, and run it.

  - `load-file` = `https://github.com/sormuras/bach/raw/master/src/bach/bach-build.jsh`
  - `load-file` = `https://sormuras.de/bach-build`

#### Run a local installment of `Bach.java`

Store a copy of `Bach.java` in your project.

  - `jshell https://sormuras.de/bach-boot`

Offers IDE support with writing your `Build.java` program.
Namespace uses `Bach.` prefix

### Module `de.sormuras.bach` API

Mount modular `de.sormuras.bach@${VERSION}.jar`.
Use its API directly in your modular build program.
