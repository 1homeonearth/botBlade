// BEGIN NEWBIE GUIDE HEADER
// This file is part of BotBlade. The goal of this header is to help brand-new developers
// understand what this file is responsible for before reading implementation details.
// Read top-to-bottom, and treat function/class names as a map of the app flow.
// If you edit behavior here, also check related tests and docs for consistency.
// END NEWBIE GUIDE HEADER
import test from "node:test";  // line 7: executes this statement as part of this file's behavior
import assert from "node:assert/strict";  // line 8: executes this statement as part of this file's behavior
import { parseFileWriteInput } from "../services/projectFiles.js";  // line 9: executes this statement as part of this file's behavior
import { RequestValidationError } from "../services/projectStore.js";  // line 10: executes this statement as part of this file's behavior

function expectValidationError(fn: () => unknown, expectedMessage: string): void {  // line 12: executes this statement as part of this file's behavior
  try {  // line 13: executes this statement as part of this file's behavior
    fn();  // line 14: executes this statement as part of this file's behavior
    assert.ok(false, "Expected RequestValidationError to be thrown.");  // line 15: executes this statement as part of this file's behavior
  } catch (error) {  // line 16: executes this statement as part of this file's behavior
    assert.ok(error instanceof RequestValidationError);  // line 17: executes this statement as part of this file's behavior
    const validationError = error as RequestValidationError;  // line 18: executes this statement as part of this file's behavior
    assert.equal(validationError.problems.some((problem: { message: string }) => problem.message === expectedMessage), true);  // line 19: executes this statement as part of this file's behavior
  }  // line 20: executes this statement as part of this file's behavior
}  // line 21: executes this statement as part of this file's behavior

test("parseFileWriteInput accepts ASCII content above 128KiB and below 512KiB", () => {  // line 23: executes this statement as part of this file's behavior
  const content = "a".repeat(300 * 1024);  // line 24: executes this statement as part of this file's behavior
  const parsed = parseFileWriteInput({ content });  // line 25: executes this statement as part of this file's behavior
  assert.equal(parsed.content.length, content.length);  // line 26: executes this statement as part of this file's behavior
});  // line 27: executes this statement as part of this file's behavior

test("parseFileWriteInput rejects content above 512KiB in UTF-8 bytes", () => {  // line 29: executes this statement as part of this file's behavior
  const content = "a".repeat((512 * 1024) + 1);  // line 30: executes this statement as part of this file's behavior
  expectValidationError(() => parseFileWriteInput({ content }), "File content exceeds 512KB limit.");  // line 31: executes this statement as part of this file's behavior
});  // line 32: executes this statement as part of this file's behavior

test("parseFileWriteInput rejects missing content", () => {  // line 34: executes this statement as part of this file's behavior
  expectValidationError(() => parseFileWriteInput({}), "File content must be a string.");  // line 35: executes this statement as part of this file's behavior
});  // line 36: executes this statement as part of this file's behavior

test("parseFileWriteInput rejects non-string content", () => {  // line 38: executes this statement as part of this file's behavior
  expectValidationError(() => parseFileWriteInput({ content: 123 }), "File content must be a string.");  // line 39: executes this statement as part of this file's behavior
});  // line 40: executes this statement as part of this file's behavior
