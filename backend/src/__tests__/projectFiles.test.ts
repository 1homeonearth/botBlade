import test from "node:test";
import assert from "node:assert/strict";
import { parseFileWriteInput } from "../services/projectFiles.js";
import { RequestValidationError } from "../services/projectStore.js";

function expectValidationError(fn: () => unknown, expectedMessage: string): void {
  try {
    fn();
    assert.ok(false, "Expected RequestValidationError to be thrown.");
  } catch (error) {
    assert.ok(error instanceof RequestValidationError);
    const validationError = error as RequestValidationError;
    assert.equal(validationError.problems.some((problem: { message: string }) => problem.message === expectedMessage), true);
  }
}

test("parseFileWriteInput accepts ASCII content above 128KiB and below 512KiB", () => {
  const content = "a".repeat(300 * 1024);
  const parsed = parseFileWriteInput({ content });
  assert.equal(parsed.content.length, content.length);
});

test("parseFileWriteInput rejects content above 512KiB in UTF-8 bytes", () => {
  const content = "a".repeat((512 * 1024) + 1);
  expectValidationError(() => parseFileWriteInput({ content }), "File content exceeds 512KB limit.");
});

test("parseFileWriteInput rejects missing content", () => {
  expectValidationError(() => parseFileWriteInput({}), "File content must be a string.");
});

test("parseFileWriteInput rejects non-string content", () => {
  expectValidationError(() => parseFileWriteInput({ content: 123 }), "File content must be a string.");
});
