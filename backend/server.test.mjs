import assert from "node:assert/strict";
import test from "node:test";
import {
  authorized,
  parseModelOutput,
  readJson,
  resetRateLimitsForTest,
  withinRateLimit,
} from "./server.mjs";

test("bearer token is required and compared exactly", () => {
  assert.equal(authorized("", "expected-token"), false);
  assert.equal(authorized("Bearer wrong", "expected-token"), false);
  assert.equal(authorized("Bearer expected-token", "expected-token"), true);
});

test("model output only accepts a bounded known risk response", () => {
  assert.deepEqual(parseModelOutput('{"level":"dangerous","reason":"诱导转账"}'), {
    level: "dangerous",
    reason: "诱导转账",
  });
  assert.throws(() => parseModelOutput('{"level":"unknown","reason":"x"}'));
  assert.throws(() => parseModelOutput('{"level":"safe","reason":""}'));
});

test("rate limit is isolated by key and resets after its window", () => {
  resetRateLimitsForTest();
  for (let index = 0; index < 30; index += 1) assert.equal(withinRateLimit("device-a", 1000), true);
  assert.equal(withinRateLimit("device-a", 1000), false);
  assert.equal(withinRateLimit("device-b", 1000), true);
  assert.equal(withinRateLimit("device-a", 61_001), true);
});

test("rate limiter also supports a global spend ceiling", () => {
  resetRateLimitsForTest();
  assert.equal(withinRateLimit("all", 1000, 2), true);
  assert.equal(withinRateLimit("all", 1000, 2), true);
  assert.equal(withinRateLimit("all", 1000, 2), false);
});

test("request reader rejects bodies above 16 KiB", async () => {
  async function* oversized() {
    yield Buffer.alloc(16 * 1024 + 1, "a");
  }
  await assert.rejects(() => readJson(oversized()), /payload_too_large/);
});
