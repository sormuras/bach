// default package

/**
 * Build program of project "Sample".
 *
 * <ul>
 *   <li>{@code src/com.acme.sample/module-info.java}
 *       <pre>
 * module com.acme.sample {
 *   requires org.apache.commons.lang3;
 *   requires org.slf4j;
 * }
 * </pre>
 *   <li>{@code src/com.acme.sample/com/acme/sample/Main.java}
 *       <pre>
 * package com.acme.sample;
 *
 * class Main {
 *   // psvm...
 * }
 * </pre>
 * </ul>
 */
class Build {
  public static void main(String... args) {
    Bach.of(
            project ->
                project
                    //
                    // Set basic project information.
                    //
                    .title("Sample Java project configured with Java")
                    .version("0.1")
                    //
                    // By default https://github.com/sormuras/modules is queried for module-to-maven
                    // coordinates mappings, including latest-and-greatest versions.
                    // It's recommended to explicitly map relations per project as shown below.
                    //
                    // .map("org.apache.commons.lang3", "org.apache.commons:commons-lang3:3.10")
                    // .map("org.slf4j", "org.slf4j:slf4j-api:2.0.0-alpha1")
                    // .map("org.slf4j.simple", "org.slf4j:slf4j-simple:2.0.0-alpha1")
                    //
                    // Download modules required at runtime
                    //
                    .requires("org.slf4j.simple"))
        .build()
        .assertSuccessful();
  }
}
