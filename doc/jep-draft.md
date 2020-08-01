# JEP draft: Java Build Tool

## Summary

Create a tool for building modular Java projects.

## Goals

Create a build tool that:

- Is a lightweight wrapper around existing and future foundation tools of the JDK.
- Can be invoked directly, from the command line, or programmatically, either via the `ToolProvider` API or via its modular API (in a JShell session).
- Infers basic project information from `module-info.java` files.
- Uses standard Java syntax for configuration purposes.
- Supports creation of MR-JAR libraries.
- Knows how to run test modules via the `ToolProvider` API.
- Optionally launches JUnit Platform-based test engines.
- Creates human-readable summary of the build process.
- Records all tool calls including names and arguments.

## Non-Goals

- There will be no support for non-modular Java projects.
- There will be no GUI for the tool; a command-line interface (CLI) is sufficient.
- There will be no support for "all features known from other build tools".

## Motivation

The JDK does not provide a build tool that guides a user from compiling Java source files to packaging an end-product.
An end-product is, for example, a modular Java library packaged in a JAR file that is reusable by other projects.

Today, users rely on external solutions that all bring in their own idea of what "Java project" is and what "build process" means.
Those external tools mainly resolve two common problems when building a Java project:

1. how to organize source assets
1. how to reuse external libraries

With Java modules introduced in Java 9, the Java language contains the basic building block to resolve both common problems in one go.

1. The location of and information contained within `module-info.java` files define the organizational structure of a modular Java project.
1. External libraries are a special case of Java modules: they are not declared within the current project but provided. 

## Description

This section outlines the functionality of the build tool.

### Build Process

1. Optional download of external modules.
1. Build main code space or "the product".
1. Build test code spaces in order to check assertions
1. Optional deployment of the product.

### New Features

There will be no support for "all features known from other build tools".

- If a feature F is not already provided by a foundation tool, this build tool will not support F.
- If F is required to build modular Java projects, F should be implemented by a foundation tool.
- If F is absolutely required to build modular Java projects and its implementation would induce changes in multiple foundation tools, this build tool will support F.


## Testing

_tbd_

## Dependencies
- Foundation tools including: `javac`, `jar`, `javadoc`, `jlink`, `jpackage` 
- Future foundation tools: `jnative` (Project Leyden)
- JUnit Platform (optional or `static`) for testing
