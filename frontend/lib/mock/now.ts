/**
 * Mock timestamps are relative to "now". Anchoring to a 10-minute bucket keeps
 * the server render and the browser hydration on the same values (otherwise
 * they differ by a few seconds and React reports a hydration mismatch).
 */
export const MOCK_NOW = Math.floor(Date.now() / 600_000) * 600_000;
