const test = require("node:test");
const assert = require("node:assert");
const crypto = require("node:crypto");
const { verifyWithKeys } = require("../ssv");

const { privateKey, publicKey } = crypto.generateKeyPairSync("ec", { namedCurve: "prime256v1" });
const keys = new Map([["123", publicKey.export({ type: "spki", format: "pem" })]]);

function signed(query) {
  const sig = crypto.sign("sha256", Buffer.from(query), privateKey).toString("base64url");
  return `${query}&signature=${sig}&key_id=123`;
}

const query = "ad_network=5450213213286189855&ad_unit=2141699138&reward_amount=1&reward_item=hour&timestamp=1800000000000&transaction_id=abc&user_id=uid123";

test("accepts a correctly signed callback", () => {
  assert.equal(verifyWithKeys(signed(query), keys), true);
});

test("verifies the decoded text when values are URL-encoded", () => {
  const decoded = "ad_network=1&ad_unit=2&custom_data=a b/c&timestamp=3&transaction_id=t&user_id=u";
  const sig = crypto.sign("sha256", Buffer.from(decoded), privateKey).toString("base64url");
  const sent = "ad_network=1&ad_unit=2&custom_data=a%20b%2Fc&timestamp=3&transaction_id=t&user_id=u";
  assert.equal(verifyWithKeys(`${sent}&signature=${sig}&key_id=123`, keys), true);
});

test("rejects a changed user id", () => {
  assert.equal(verifyWithKeys(signed(query).replace("uid123", "attacker"), keys), false);
});

test("rejects an unknown key or missing signature", () => {
  assert.equal(verifyWithKeys(signed(query).replace("key_id=123", "key_id=9"), keys), false);
  assert.equal(verifyWithKeys(query, keys), false);
});
