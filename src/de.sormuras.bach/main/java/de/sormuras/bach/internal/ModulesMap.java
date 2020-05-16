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

package de.sormuras.bach.internal;

import java.util.Locale;
import java.util.TreeMap;

/** Mutable map of module names to mostly Maven-based coordinate mappings. */
public /*static*/ class ModulesMap extends TreeMap<String, String> {
  private static final long serialVersionUID = -7978021121082640440L;

  public static String platform(String linux, String mac, String windows) {
    var os = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
    return os.contains("win") ? windows : os.contains("mac") ? mac : linux;
  }

  public ModulesMap() {
    put(
        "javafx.base",
        platform(
            "https://repo.maven.apache.org/maven2/org/openjfx/javafx-base/14.0.1/javafx-base-14.0.1-linux.jar",
            "https://repo.maven.apache.org/maven2/org/openjfx/javafx-base/14.0.1/javafx-base-14.0.1-mac.jar",
            "https://repo.maven.apache.org/maven2/org/openjfx/javafx-base/14.0.1/javafx-base-14.0.1-win.jar"));
    put(
        "javafx.controls",
        platform(
            "https://repo.maven.apache.org/maven2/org/openjfx/javafx-controls/14.0.1/javafx-controls-14.0.1-linux.jar",
            "https://repo.maven.apache.org/maven2/org/openjfx/javafx-controls/14.0.1/javafx-controls-14.0.1-mac.jar",
            "https://repo.maven.apache.org/maven2/org/openjfx/javafx-controls/14.0.1/javafx-controls-14.0.1-win.jar"));
    put(
        "javafx.fxml",
        platform(
            "https://repo.maven.apache.org/maven2/org/openjfx/javafx-fxml/14.0.1/javafx-fxml-14.0.1-linux.jar",
            "https://repo.maven.apache.org/maven2/org/openjfx/javafx-fxml/14.0.1/javafx-fxml-14.0.1-mac.jar",
            "https://repo.maven.apache.org/maven2/org/openjfx/javafx-fxml/14.0.1/javafx-fxml-14.0.1-win.jar"));
    put(
        "javafx.graphics",
        platform(
            "https://repo.maven.apache.org/maven2/org/openjfx/javafx-graphics/14.0.1/javafx-graphics-14.0.1-linux.jar",
            "https://repo.maven.apache.org/maven2/org/openjfx/javafx-graphics/14.0.1/javafx-graphics-14.0.1-mac.jar",
            "https://repo.maven.apache.org/maven2/org/openjfx/javafx-graphics/14.0.1/javafx-graphics-14.0.1-win.jar"));
    put(
        "javafx.media",
        platform(
            "https://repo.maven.apache.org/maven2/org/openjfx/javafx-media/14.0.1/javafx-media-14.0.1-linux.jar",
            "https://repo.maven.apache.org/maven2/org/openjfx/javafx-media/14.0.1/javafx-media-14.0.1-mac.jar",
            "https://repo.maven.apache.org/maven2/org/openjfx/javafx-media/14.0.1/javafx-media-14.0.1-win.jar"));
    put(
        "javafx.swing",
        platform(
            "https://repo.maven.apache.org/maven2/org/openjfx/javafx-swing/14.0.1/javafx-swing-14.0.1-linux.jar",
            "https://repo.maven.apache.org/maven2/org/openjfx/javafx-swing/14.0.1/javafx-swing-14.0.1-mac.jar",
            "https://repo.maven.apache.org/maven2/org/openjfx/javafx-swing/14.0.1/javafx-swing-14.0.1-win.jar"));
    put(
        "javafx.web",
        platform(
            "https://repo.maven.apache.org/maven2/org/openjfx/javafx-web/14.0.1/javafx-web-14.0.1-linux.jar",
            "https://repo.maven.apache.org/maven2/org/openjfx/javafx-web/14.0.1/javafx-web-14.0.1-mac.jar",
            "https://repo.maven.apache.org/maven2/org/openjfx/javafx-web/14.0.1/javafx-web-14.0.1-win.jar"));
    put("junit", "https://repo.maven.apache.org/maven2/junit/junit/4.13/junit-4.13.jar");
    put(
        "net.bytebuddy",
        "https://repo.maven.apache.org/maven2/net/bytebuddy/byte-buddy/1.10.10/byte-buddy-1.10.10.jar");
    put(
        "net.bytebuddy.agent",
        "https://repo.maven.apache.org/maven2/net/bytebuddy/byte-buddy-agent/1.10.10/byte-buddy-agent-1.10.10.jar");
    put(
        "org.apiguardian.api",
        "https://repo.maven.apache.org/maven2/org/apiguardian/apiguardian-api/1.1.0/apiguardian-api-1.1.0.jar");
    put(
        "org.assertj.core",
        "https://repo.maven.apache.org/maven2/org/assertj/assertj-core/3.16.1/assertj-core-3.16.1.jar");
    put(
        "org.hamcrest",
        "https://repo.maven.apache.org/maven2/org/hamcrest/hamcrest/2.2/hamcrest-2.2.jar");
    put(
        "org.junit.jupiter",
        "https://repo.maven.apache.org/maven2/org/junit/jupiter/junit-jupiter/5.7.0-M1/junit-jupiter-5.7.0-M1.jar");
    put(
        "org.junit.jupiter.api",
        "https://repo.maven.apache.org/maven2/org/junit/jupiter/junit-jupiter-api/5.7.0-M1/junit-jupiter-api-5.7.0-M1.jar");
    put(
        "org.junit.jupiter.engine",
        "https://repo.maven.apache.org/maven2/org/junit/jupiter/junit-jupiter-engine/5.7.0-M1/junit-jupiter-engine-5.7.0-M1.jar");
    put(
        "org.junit.jupiter.params",
        "https://repo.maven.apache.org/maven2/org/junit/jupiter/junit-jupiter-params/5.7.0-M1/junit-jupiter-params-5.7.0-M1.jar");
    put(
        "org.junit.platform.commons",
        "https://repo.maven.apache.org/maven2/org/junit/platform/junit-platform-commons/1.7.0-M1/junit-platform-commons-1.7.0-M1.jar");
    put(
        "org.junit.platform.console",
        "https://repo.maven.apache.org/maven2/org/junit/platform/junit-platform-console/1.7.0-M1/junit-platform-console-1.7.0-M1.jar");
    put(
        "org.junit.platform.engine",
        "https://repo.maven.apache.org/maven2/org/junit/platform/junit-platform-engine/1.7.0-M1/junit-platform-engine-1.7.0-M1.jar");
    put(
        "org.junit.platform.launcher",
        "https://repo.maven.apache.org/maven2/org/junit/platform/junit-platform-launcher/1.7.0-M1/junit-platform-launcher-1.7.0-M1.jar");
    put(
        "org.junit.platform.reporting",
        "https://repo.maven.apache.org/maven2/org/junit/platform/junit-platform-reporting/1.7.0-M1/junit-platform-reporting-1.7.0-M1.jar");
    put(
        "org.junit.platform.testkit",
        "https://repo.maven.apache.org/maven2/org/junit/platform/junit-platform-testkit/1.7.0-M1/junit-platform-testkit-1.7.0-M1.jar");
    put(
        "org.junit.vintage.engine",
        "https://repo.maven.apache.org/maven2/org/junit/vintage/junit-vintage-engine/5.7.0-M1/junit-vintage-engine-5.7.0-M1.jar");
    put(
        "org.objectweb.asm",
        "https://repo.maven.apache.org/maven2/org/ow2/asm/asm/8.0.1/asm-8.0.1.jar");
    put(
        "org.objectweb.asm.commons",
        "https://repo.maven.apache.org/maven2/org/ow2/asm/asm-commons/8.0.1/asm-commons-8.0.1.jar");
    put(
        "org.objectweb.asm.tree",
        "https://repo.maven.apache.org/maven2/org/ow2/asm/asm-tree/8.0.1/asm-tree-8.0.1.jar");
    put(
        "org.objectweb.asm.tree.analysis",
        "https://repo.maven.apache.org/maven2/org/ow2/asm/asm-analysis/8.0.1/asm-analysis-8.0.1.jar");
    put(
        "org.objectweb.asm.util",
        "https://repo.maven.apache.org/maven2/org/ow2/asm/asm-util/8.0.1/asm-util-8.0.1.jar");
    put(
        "org.opentest4j",
        "https://repo.maven.apache.org/maven2/org/opentest4j/opentest4j/1.2.0/opentest4j-1.2.0.jar");
  }
}
