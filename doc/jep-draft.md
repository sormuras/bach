# JEP draft: Java Build Tool

## Summary

Create a tool for building modular Java projects.

## History and Status Quo

The JDK does not provide a build tool that guides a user from compiling Java source files to packaging an end-product.

Today, users rely on external solutions that all bring in their own idea of what "Java project" is and what "build process" means.
Those external tools mainly resolve two common problems when building a Java project:

1. how to organize source assets
2. how to reuse external libraries

With Java modules introduced in Java 9, the Java language contains the basic building block to resolve both common problems in one go.

1. The location of and information contained within `module-info.java` files define the organizational structure of a modular Java project.
2. External libraries are a special case of Java modules: they are not declared within the current project but provided. 

## Goals

Create a build tool that:

- Is a lightweight wrapper around existing and future foundation tools of the JDK.
- Can be invoked directly, from the command line, or programmatically, either via the `ToolProvider` API or via its class-based API.
- Infers basic project information from `module-info.java` files
- Uses standard the Java syntax for configuration purposes
- Supports creation of MR-JAR libraries
- Knows how to run test modules via the `ToolProvider` API
- optionally launches JUnit Platform-based test engines

## Non-Goals

- There will be no support for non-modular Java projects.
- There will be no GUI for the tool; a command-line interface (CLI) is sufficient.
- There will be no support for "features known from other build tools", if those features are not already provided by a underlying foundation tool.

## Motivation

_tbd_

## Description

_tbd_

## Testing

_tbd_

## Dependencies
- Foundation tools including: `javac`, `jar`, `javadoc`, `jlink`, `jpackage`, `jnative` (Project Leiden)
- Future foundation tools
- JUnit Platform (optional or `static`)
