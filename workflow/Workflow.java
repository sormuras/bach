/*
 * Copyright (c) 2024 Christian Stein
 * Licensed under the Universal Permissive License v 1.0 -> https://opensource.org/license/upl
 */

package run.bach.workflow;

import run.bach.Bach;
import run.bach.ToolRunner;

public record Workflow(Bach.Folders folders, Structure structure, ToolRunner runner) {}
