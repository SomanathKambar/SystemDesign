import type { ExperimentMetadata, RateLimitEvent } from "../types";

export class ExperimentLoader {
  static async loadMetadata(strategy: string, id: string): Promise<ExperimentMetadata> {
    const response = await fetch(`/experiments/${strategy}/${id}/metadata.json`);
    if (!response.ok) throw new Error(`Failed to load metadata for ${id}`);
    return response.json();
  }

  static async loadEvents(strategy: string, id: string): Promise<RateLimitEvent[]> {
    const response = await fetch(`/experiments/${strategy}/${id}/events.jsonl`);
    if (!response.ok) throw new Error(`Failed to load events for ${id}`);
    const text = await response.text();
    return text
      .split("\n")
      .filter((line) => line.trim() !== "")
      .map((line) => JSON.parse(line) as RateLimitEvent);
  }
}