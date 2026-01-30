export interface RateLimitEvent {
  type: string;
  eventId: string;
  timestampMs: number;
  strategy: string;
  nodeId: string;
  payload: Record<string, string>;
  // Strategy specific fields (some may be in payload depending on how we want to parse it)
  tokensAdded?: number;
  currentTokens?: number;
  newWindowStartMs?: number;
  reason?: string;
  leakedAmount?: number;
  waterLevelAfterLeak?: number;
}

export interface TrafficProfile {
  type: string;
  name: string;
  requestsPerSecond?: number;
  durationMs: number;
  burstSize?: number;
  intervalMs?: number;
  avgRequestsPerSecond?: number;
}

export interface ExperimentMetadata {
  id: string;
  name: string;
  description: string;
  strategy: string;
  config: Record<string, string>;
  profile: TrafficProfile;
  timestamp: number;
}
