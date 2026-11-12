const { onRequest } = require("firebase-functions/v2/https");
const logger = require("firebase-functions/logger");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore, Timestamp } = require("firebase-admin/firestore");
const functionsV1 = require("firebase-functions/v1");
const { getAuth } = require("firebase-admin/auth");
const { verifyCallback } = require("./ssv");
const { planFounders, FOUNDING_LIMIT } = require("./founding");
const { grantAdHour } = require("./adHours");

initializeApp();
const db = getFirestore();

/**
 * AdMob rewarded-ad callback. The app sets the signed-in Firebase uid as the SSV user id, so a
 * verified callback grants that account one hour of Pro. Only this function writes Pro time;
 * Firestore rules stop the app from writing it.
 */
exports.admobReward = onRequest({ region: "us-central1", invoker: "public", maxInstances: 5 }, async (req, res) => {
  const rawQuery = req.originalUrl.includes("?") ? req.originalUrl.slice(req.originalUrl.indexOf("?") + 1) : "";
  const uid = req.query.user_id;
  const transactionId = req.query.transaction_id;

  const verified = await verifyCallback(rawQuery).catch((e) => {
    logger.error("SSV key fetch failed", e);
    return null;
  });
  if (verified === null) {
    // Couldn't reach Google's keys: 503 so AdMob retries later
    res.status(503).send("try again");
    return;
  }
  if (!verified) {
    logger.warn("SSV signature rejected", {
      originalUrl: req.originalUrl.slice(0, 600),
      keyId: req.query.key_id,
      params: Object.keys(req.query),
    });
    res.status(rawQuery ? 403 : 400).send("invalid signature");
    return;
  }
  // AdMob's "Verify URL" check in the console sends a signed request without a user
  if (!uid || !transactionId) {
    res.status(200).send("ok");
    return;
  }

  const userRef = db.collection("users").doc(String(uid));
  const result = await db.runTransaction(async (tx) => {
    const snap = await tx.get(userRef);
    if (!snap.exists) return { granted: false, reason: "no_user" };
    const data = snap.data();
    const now = Date.now();
    const grant = grantAdHour(
      {
        untilMillis: data.proUntil?.toMillis?.() ?? 0,
        adTimes: (data.proAdTimes || []).map((t) => t.toMillis()),
        transactions: data.proAdTxns || [],
      },
      now,
      String(transactionId),
    );
    if (grant.granted) {
      tx.update(userRef, {
        proUntil: Timestamp.fromMillis(grant.untilMillis),
        proAdTimes: grant.adTimes.map((t) => Timestamp.fromMillis(t)),
        proAdTxns: grant.transactions,
      });
    }
    return grant;
  });

  logger.info("ad reward", { uid, transactionId, granted: result.granted, reason: result.reason });
  // 200 either way: the callback was genuine, retrying won't change a limit
  res.status(200).send(result.granted ? "granted" : result.reason);
});


// --- Founding members ---------------------------------------------------------------------------
// The number lives in founders/{uid}, not in the profile: every app version writes the profile
// (1.6.0 sign-up even replaces it whole), and no app version writes here.

const foundingMeta = () => db.collection("meta").doc("founding");
const founderRef = (uid) => db.collection("founders").doc(uid);

/**
 * Numbers the first accounts once, in sign-up order, so members who joined before this function
 * existed get theirs too. Safe to run again: it only fills numbers that are missing, up to the limit.
 */
async function backfillFounders() {
  const meta = await foundingMeta().get();
  if (meta.exists && meta.data().backfilled) return { backfilled: false, count: meta.data().count };

  const users = [];
  let pageToken;
  do {
    const page = await getAuth().listUsers(1000, pageToken);
    page.users.forEach((u) => users.push({ uid: u.uid, created: Date.parse(u.metadata.creationTime) }));
    pageToken = page.pageToken;
  } while (pageToken);

  return db.runTransaction(async (tx) => {
    const fresh = await tx.get(foundingMeta());
    if (fresh.exists && fresh.data().backfilled) return { backfilled: false, count: fresh.data().count };
    const docs = users.length ? await tx.getAll(...users.map((u) => founderRef(u.uid))) : [];
    const assigned = new Map();
    docs.forEach((d) => { if (d.exists) assigned.set(d.id, d.data().number); });
    const plan = planFounders(users, assigned);
    plan.forEach(({ uid, number }) => tx.set(founderRef(uid), { number, at: Timestamp.now() }));
    const count = Math.max(0, ...assigned.values(), ...plan.map((p) => p.number));
    tx.set(foundingMeta(), { count, limit: FOUNDING_LIMIT, backfilled: true }, { merge: true });
    return { backfilled: true, count, assigned: plan.length };
  });
}

/** Gives a new account the next founding number while fewer than FOUNDING_LIMIT have been given. */
exports.assignFoundingNumber = functionsV1.region("us-central1").auth.user().onCreate(async (user) => {
  await backfillFounders(); // first run numbers everyone who signed up before; it may include this user
  const result = await db.runTransaction(async (tx) => {
    const meta = await tx.get(foundingMeta());
    const existing = await tx.get(founderRef(user.uid));
    if (existing.exists) return "already";
    const count = meta.exists ? meta.data().count || 0 : 0;
    if (count >= FOUNDING_LIMIT) return "full";
    tx.set(founderRef(user.uid), { number: count + 1, at: Timestamp.now() });
    tx.set(foundingMeta(), { count: count + 1, limit: FOUNDING_LIMIT, backfilled: true }, { merge: true });
    return `#${count + 1}`;
  });
  logger.info("founding", { uid: user.uid, result });
});

/** A deleted account's number goes with it; it is never given to anyone else. */
exports.removeFounder = functionsV1.region("us-central1").auth.user().onDelete(async (user) => {
  await founderRef(user.uid).delete();
});

/**
 * Runs the one-time numbering of existing accounts right away instead of waiting for the next
 * sign-up. Harmless to call by anyone: it only fills missing numbers in sign-up order.
 */
exports.foundingBackfill = onRequest({ region: "us-central1", invoker: "public", maxInstances: 1 }, async (req, res) => {
  res.json(await backfillFounders());
});
