const test = require("node:test");
const assert = require("node:assert");
const { grantAdHour, HOUR, DAY } = require("../adHours");

const now = 1_800_000_000_000;

test("first ad gives one hour from now", () => {
  const g = grantAdHour({}, now, "t1");
  assert.equal(g.granted, true);
  assert.equal(g.untilMillis, now + HOUR);
});

test("ads stack on top of the time left", () => {
  const g = grantAdHour({ untilMillis: now + 3 * HOUR, adTimes: [now - HOUR] }, now, "t2");
  assert.equal(g.untilMillis, now + 4 * HOUR);
  assert.equal(g.adTimes.length, 2);
});

test("bank is full when another hour would pass 8", () => {
  assert.equal(grantAdHour({ untilMillis: now + 7.5 * HOUR }, now, "t").reason, "bank_full");
  assert.equal(grantAdHour({ untilMillis: now + 7 * HOUR }, now, "t").granted, true);
});

test("five ads per rolling 24 hours", () => {
  const five = [1, 2, 3, 4, 5].map((h) => now - h * HOUR);
  assert.equal(grantAdHour({ adTimes: five }, now, "t").reason, "daily_limit");
  const oneOld = [...five.slice(1), now - DAY - 1];
  assert.equal(grantAdHour({ adTimes: oneOld }, now, "t").granted, true);
});

test("a retried callback is granted once", () => {
  assert.equal(grantAdHour({ transactions: ["t1"] }, now, "t1").reason, "duplicate");
});
