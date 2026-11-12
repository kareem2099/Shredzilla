const test = require("node:test");
const assert = require("node:assert");
const { planFounders } = require("../founding");

const users = [
  { uid: "c", created: 300 },
  { uid: "a", created: 100 },
  { uid: "b", created: 200 },
];

test("numbers follow sign-up order", () => {
  assert.deepEqual(planFounders(users, new Map()), [
    { uid: "a", number: 1 }, { uid: "b", number: 2 }, { uid: "c", number: 3 },
  ]);
});

test("existing numbers are kept and the rest continue after them", () => {
  assert.deepEqual(planFounders(users, new Map([["a", 1]])), [
    { uid: "b", number: 2 }, { uid: "c", number: 3 },
  ]);
});

test("stops at the limit", () => {
  assert.deepEqual(planFounders(users, new Map(), 2), [
    { uid: "a", number: 1 }, { uid: "b", number: 2 },
  ]);
  assert.deepEqual(planFounders(users, new Map([["a", 1], ["b", 2]]), 2), []);
});
