/*
 * Copyright (c) 2024 Christian Stein
 * Licensed under the Universal Permissive License v 1.0 -> https://opensource.org/license/upl
 */

package run.bach;

/** Recorded data of a tool run. */
public record ToolRun(ToolCall call, Tool tool, int code, String out, String err) {}
