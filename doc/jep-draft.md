# Java Build Tool

## The Problem

The JDK does not provide a build tool that guides a user from compiling Java source files to packaging an end-product.
Today, users rely on external solutions that all bring in their own idea of what "Java project" is and what "build process" means.
Those external tools mainly resolve two common problems when building a Java project:

1. how to organize source assets
2. how to reuse external libraries

## The JDK should solve this problem

With Java modules introduced in Java 9, the Java language contains the basic building block to resolve both common problems in one go.

1. The location of and information contained within `module-info.java` files define the organizational structure of a modular Java project.
2. External libraries are a special case of Java modules: they are not declared within the current project but provided. 

## Possible Solutions

### External Build Tools

Adopt make, Ant, Maven, and many more.

### A JDK Build Tool

## The Best Solution

