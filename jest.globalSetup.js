// Jest workers inherit this environment, so `new Date(y, m, d)` resolves in Lebanon's time zone
// (DST: last Sunday of March / October at 00:00) on every machine. The countdown tests depend on it.
module.exports = async () => {
  process.env.TZ = 'Asia/Beirut';
};
