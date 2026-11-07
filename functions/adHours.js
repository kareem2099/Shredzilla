/**
 * Pro hours from rewarded ads. Mirrors the app's limits; the server is the only writer, using its own clock.
 * Kept free of Firebase so it can be unit tested.
 */
const HOUR = 60 * 60 * 1000;
const DAY = 24 * HOUR;
const HOURS_PER_AD = 1;
const MAX_BANKED_HOURS = 8;
const MAX_ADS_PER_DAY = 5; // rolling 24 hours, so changing the phone's clock or time zone doesn't reset it
const KEEP_TRANSACTIONS = 20;

/**
 * @param {{untilMillis?: number, adTimes?: number[], transactions?: string[]}} state current user-doc values
 * @param {number} now server time in ms
 * @param {string} transactionId AdMob's id for this reward, to ignore retried callbacks
 */
function grantAdHour(state, now, transactionId) {
  const transactions = state.transactions || [];
  if (transactions.includes(transactionId)) return { granted: false, reason: "duplicate" };

  const recent = (state.adTimes || []).filter((t) => now - t < DAY);
  if (recent.length >= MAX_ADS_PER_DAY) return { granted: false, reason: "daily_limit" };

  const until = state.untilMillis || 0;
  const remaining = Math.max(0, until - now);
  if (remaining + HOURS_PER_AD * HOUR > MAX_BANKED_HOURS * HOUR) return { granted: false, reason: "bank_full" };

  return {
    granted: true,
    untilMillis: Math.min(Math.max(now, until) + HOURS_PER_AD * HOUR, now + MAX_BANKED_HOURS * HOUR),
    adTimes: [...recent, now],
    transactions: [...transactions, transactionId].slice(-KEEP_TRANSACTIONS),
  };
}

module.exports = { grantAdHour, HOUR, DAY, MAX_ADS_PER_DAY, MAX_BANKED_HOURS };
