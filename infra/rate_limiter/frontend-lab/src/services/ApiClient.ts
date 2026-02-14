export interface ConfigRequest {
    limit: number;
    windowSizeMs: number;
    strategy: string;
    capacity: number;
    refillTokensPerSecond: number;
    leakRate: number;
    simulationMode: boolean;
}

export class ApiClient {
    static async getConfig(): Promise<ConfigRequest> {
        const response = await fetch('/api/config');
        return response.json();
    }

    static async updateConfig(config: ConfigRequest): Promise<any> {
        const response = await fetch('/api/config', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(config)
        });
        return response.json();
    }

    static async sendRequest(key: string): Promise<any> {
        const response = await fetch('/api/request', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ key })
        });
        return response.json();
    }
}
