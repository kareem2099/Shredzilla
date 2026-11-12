/**
 * Founding members: the first FOUNDING_LIMIT accounts, numbered #1, #2… in the order they were
 * created. Kept free of Firebase so it can be unit tested.
 */
const FOUNDING_LIMIT = 100;

/**
 * Numbers for accounts that don't have one yet, in creation order, after the highest number in use.
 * @param {{uid: string, created: number}[]} users every account, any order
 * @param {Map<string, number>} assigned uid -> number already given
 */
function planFounders(users, assigned, limit = FOUNDING_LIMIT) {
  let next = Math.max(0, ...assigned.values()) + 1;
  const plan = [];
  for (const user of [...users].sort((a, b) => a.created - b.created || a.uid.localeCompare(b.uid))) {
    if (next > limit) break;
    if (assigned.has(user.uid)) continue;
    plan.push({ uid: user.uid, number: next++ });
  }
  return plan;
}

module.exports = { planFounders, FOUNDING_LIMIT };
