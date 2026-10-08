module.exports = {
  testEnvironment: 'jsdom',
  moduleFileExtensions: ['js', 'json', 'vue'],
  transform: {
    '^.+\\.vue$': '@vue/vue2-jest',
    '^.+\\.js$': 'babel-jest'
  },
  moduleNameMapper: {
    '^@/(.*)$': '<rootDir>/src/$1'
  },
  testMatch: ['**/tests/**/*.spec.js'],
  collectCoverageFrom: [
    'src/components/**/*.vue',
    'src/views/**/*.vue'
  ],
  coverageDirectory: 'coverage',
  coverageReporters: ['text', 'lcov']
};
