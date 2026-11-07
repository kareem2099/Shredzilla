/**
 * AdMob server-side verification (SSV): AdMob calls our URL after a rewarded ad was watched,
 * signing the query string with an ECDSA key that Google publishes.
 * https://developers.google.com/admob/android/ssv
 */
const crypto = require("node:crypto");

const KEYS_URL = "https://www.gstatic.com/admob/reward/verifier-keys.json";
const KEY_CACHE_MS = 24 * 60 * 60 * 1000;

let cachedKeys = null;
let cachedAt = 0;

async function fetchKeys(force = false) {
  if (!force && cachedKeys && Date.now() - cachedAt < KEY_CACHE_MS) return cachedKeys;
  const res = await fetch(KEYS_URL);
  if (!res.ok) throw new Error(`verifier keys: HTTP ${res.status}`);
  const body = await res.json();
  cachedKeys = new Map(body.keys.map((k) => [String(k.keyId), k.pem]));
  cachedAt = Date.now();
  return cachedKeys;
}

/**
 * Splits the raw query string into the signed part and the signature. The signed content is
 * everything before "&signature=", percent-decoded the way Google's own verifier reads it
 * (Java's URI.getQuery(), which leaves "+" alone).
 */
function splitSignedQuery(rawQuery) {
  const marker = rawQuery.indexOf("&signature=");
  if (marker < 0) return null;
  let message;
  try {
    message = decodeURIComponent(rawQuery.slice(0, marker));
  } catch {
    return null;
  }
  const params = new URLSearchParams(rawQuery.slice(marker + 1));
  const signature = params.get("signature");
  const keyId = params.get("key_id");
  if (!signature || !keyId) return null;
  return { message, signature, keyId };
}

/** True when [rawQuery] carries a valid signature from one of [keys] (keyId -> PEM). */
function verifyWithKeys(rawQuery, keys) {
  const parts = splitSignedQuery(rawQuery);
  if (!parts) return false;
  const pem = keys.get(parts.keyId);
  if (!pem) return false;
  try {
    return crypto.verify("sha256", Buffer.from(parts.message, "utf8"), pem, Buffer.from(parts.signature, "base64url"));
  } catch {
    return false;
  }
}

async function verifyCallback(rawQuery) {
  const parts = splitSignedQuery(rawQuery);
  if (!parts) return false;
  let keys = await fetchKeys();
  // Google rotates keys; refetch once when the id is new to us
  if (!keys.has(parts.keyId)) keys = await fetchKeys(true);
  return verifyWithKeys(rawQuery, keys);
}

module.exports = { verifyCallback, verifyWithKeys, splitSignedQuery };
