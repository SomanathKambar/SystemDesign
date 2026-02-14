import type { RateLimitEvent } from "../types";

export class TelemetryClient {
    private socket: WebSocket | null = null;
    private onEventCallback: (event: RateLimitEvent) => void;

    constructor(onEvent: (event: RateLimitEvent) => void) {
        this.onEventCallback = onEvent;
    }

    connect() {
        const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
        const host = window.location.host;
        const wsUrl = `${protocol}//${host}/ws/telemetry`;
        
        console.log(`Connecting to telemetry at ${wsUrl}`);
        this.socket = new WebSocket(wsUrl);

        this.socket.onmessage = (event) => {
            try {
                const data = JSON.parse(event.data) as RateLimitEvent;
                this.onEventCallback(data);
            } catch (e) {
                console.error("Failed to parse telemetry event", e);
            }
        };

        this.socket.onclose = () => {
            console.log("Telemetry connection closed. Reconnecting in 5s...");
            setTimeout(() => this.connect(), 5000);
        };

        this.socket.onerror = (error) => {
            console.error("Telemetry socket error", error);
        };
    }

    disconnect() {
        if (this.socket) {
            this.socket.close();
            this.socket = null;
        }
    }
}
