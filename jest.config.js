module.exports = {
  preset: '@react-native/jest-preset',
  testPathIgnorePatterns: ['/node_modules/', '/android/', '/ios/'],
  // Runs every test in Asia/Beirut, so the DST cases cross a real daylight-saving change.
  globalSetup: './jest.globalSetup.js',
};
