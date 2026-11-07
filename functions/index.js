const { onRequest } = require("firebase-functions/v2/https");
const logger = require("firebase-functions/logger");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore, Timestamp } = require("firebase-admin/firestore");
const { verifyCallback } = require("./ssv");
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
