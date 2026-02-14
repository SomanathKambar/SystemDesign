import { useState, useEffect, useRef } from 'react';
import { ApiClient } from '../services/ApiClient';
import type { ConfigRequest } from '../services/ApiClient';
import { TelemetryClient } from '../services/TelemetryClient';
import type { RateLimitEvent } from '../types';
import { TokenBucketVisualizer } from './TokenBucketVisualizer';
import { LeakyBucketVisualizer } from './LeakyBucketVisualizer';
import { FixedWindowVisualizer } from './FixedWindowVisualizer';
import { SlidingWindowVisualizer } from './SlidingWindowVisualizer';
import { ThroughputChart } from './ThroughputChart';

export function LiveDashboard() {
    const [config, setConfig] = useState<ConfigRequest | null>(null);
    const [events, setEvents] = useState<RateLimitEvent[]>([]);
    const [currentTime, setCurrentTime] = useState(0);
    const startTimeRef = useRef(Date.now());
    const telemetryClientRef = useRef<TelemetryClient | null>(null);

    useEffect(() => {
        ApiClient.getConfig().then(setConfig);

        const tc = new TelemetryClient((event) => {
            setEvents(prev => [...prev.slice(-100), event]);
        });
        tc.connect();
        telemetryClientRef.current = tc;

        const timer = setInterval(() => {
            setCurrentTime(Date.now() - startTimeRef.current);
        }, 50);

        return () => {
            tc.disconnect();
            clearInterval(timer);
        };
    }, []);

    const handleUpdateConfig = async (updates: Partial<ConfigRequest>) => {
        if (!config) return;
        const newConfig = { ...config, ...updates };
        const result = await ApiClient.updateConfig(newConfig);
        setConfig(result.config);
        // Reset view for new config
        setEvents([]);
        startTimeRef.current = Date.now();
        setCurrentTime(0);
    };

    const sendRequest = () => {
        ApiClient.sendRequest('user-1');
    };

    if (!config) return <div>Loading Live Engine...</div>;

    const renderVisualizer = () => {
        const visualizerConfig = {
            limit: config.limit.toString(),
            windowSizeMs: config.windowSizeMs.toString(),
            capacity: config.capacity.toString(),
            rate: config.refillTokensPerSecond.toString(),
            leakRate: config.leakRate.toString(),
            duration: "60000" // 1 minute rolling
        };

        switch (config.strategy) {
            case 'TOKEN_BUCKET':
                return <TokenBucketVisualizer currentTime={currentTime} events={events} config={visualizerConfig} />
            case 'LEAKY_BUCKET':
                return <LeakyBucketVisualizer currentTime={currentTime} events={events} config={visualizerConfig} />
            case 'FIXED_WINDOW':
                return <FixedWindowVisualizer currentTime={currentTime} events={events} config={visualizerConfig} />
            case 'SLIDING_WINDOW_COUNTER':
            case 'SLIDING_WINDOW_LOG':
                return <SlidingWindowVisualizer currentTime={currentTime} events={events} config={visualizerConfig} />
            default:
                return <div>Strategy {config.strategy} not supported in live view yet.</div>;
        }
    };

    return (
        <div className="flex flex-col gap-6 w-full h-full p-4">
            <div className="flex-1 flex flex-col items-center justify-center min-h-[400px]">
                {renderVisualizer()}
                <div className="w-full max-w-lg mt-8">
                    <ThroughputChart 
                        events={events} 
                        windowSizeMs={config.windowSizeMs} 
                        limit={config.limit} 
                    />
                </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-6 bg-slate-900/80 p-6 rounded-3xl border border-white/5">
                <div className="space-y-4">
                    <h3 className="text-xs font-black text-cyan-500 uppercase tracking-[0.2em]">Control Law Configuration</h3>
                    
                    <div className="grid grid-cols-2 gap-4">
                        <div>
                            <label className="text-[10px] text-slate-500 uppercase block mb-1">Strategy</label>
                            <select 
                                value={config.strategy}
                                onChange={(e) => handleUpdateConfig({ strategy: e.target.value })}
                                className="w-full bg-slate-950 border border-white/10 text-xs text-white p-2 rounded-lg"
                            >
                                <option value="FIXED_WINDOW">Fixed Window</option>
                                <option value="SLIDING_WINDOW_COUNTER">Sliding Window (Counter)</option>
                                <option value="SLIDING_WINDOW_LOG">Sliding Window (Log)</option>
                                <option value="TOKEN_BUCKET">Token Bucket</option>
                                <option value="LEAKY_BUCKET">Leaky Bucket</option>
                            </select>
                        </div>
                        <div>
                            <label className="text-[10px] text-slate-500 uppercase block mb-1">Mode</label>
                            <button 
                                onClick={() => handleUpdateConfig({ simulationMode: !config.simulationMode })}
                                className={`w-full text-xs p-2 rounded-lg font-bold border transition-all ${
                                    config.simulationMode 
                                    ? 'bg-fuchsia-500/10 border-fuchsia-500/50 text-fuchsia-400' 
                                    : 'bg-green-500/10 border-green-500/50 text-green-400'
                                }`}
                            >
                                {config.simulationMode ? 'SIMULATION (Explorer)' : 'OPERATIONAL (Governed)'}
                            </button>
                        </div>
                    </div>

                    <div className="grid grid-cols-2 gap-4">
                        <div>
                            <label className="text-[10px] text-slate-500 uppercase block mb-1">Limit (Reqs/Window)</label>
                            <input 
                                type="number" 
                                value={config.limit}
                                onChange={(e) => handleUpdateConfig({ limit: parseInt(e.target.value) })}
                                className="w-full bg-slate-950 border border-white/10 text-xs text-white p-2 rounded-lg"
                            />
                        </div>
                        <div>
                            <label className="text-[10px] text-slate-500 uppercase block mb-1">Window (ms)</label>
                            <input 
                                type="number" 
                                value={config.windowSizeMs}
                                onChange={(e) => handleUpdateConfig({ windowSizeMs: parseInt(e.target.value) })}
                                className="w-full bg-slate-950 border border-white/10 text-xs text-white p-2 rounded-lg"
                            />
                        </div>
                    </div>

                    {(config.strategy === 'TOKEN_BUCKET' || config.strategy === 'LEAKY_BUCKET') && (
                        <div className="grid grid-cols-2 gap-4">
                            <div>
                                <label className="text-[10px] text-slate-500 uppercase block mb-1">Capacity</label>
                                <input 
                                    type="number" 
                                    value={config.capacity}
                                    onChange={(e) => handleUpdateConfig({ capacity: parseFloat(e.target.value) })}
                                    className="w-full bg-slate-950 border border-white/10 text-xs text-white p-2 rounded-lg"
                                />
                            </div>
                            <div>
                                <label className="text-[10px] text-slate-500 uppercase block mb-1">
                                    {config.strategy === 'TOKEN_BUCKET' ? 'Refill Rate (tok/s)' : 'Leak Rate (tok/s)'}
                                </label>
                                <input 
                                    type="number" 
                                    value={config.strategy === 'TOKEN_BUCKET' ? config.refillTokensPerSecond : config.leakRate}
                                    onChange={(e) => {
                                        const val = parseFloat(e.target.value);
                                        if (config.strategy === 'TOKEN_BUCKET') {
                                            handleUpdateConfig({ refillTokensPerSecond: val });
                                        } else {
                                            handleUpdateConfig({ leakRate: val });
                                        }
                                    }}
                                    className="w-full bg-slate-950 border border-white/10 text-xs text-white p-2 rounded-lg"
                                />
                            </div>
                        </div>
                    )}
                </div>

                <div className="flex flex-col justify-center items-center gap-6 border-l border-white/5 pl-6">
                    <h3 className="text-xs font-black text-fuchsia-500 uppercase tracking-[0.2em]">Manual Force</h3>
                    <button 
                        onClick={sendRequest}
                        className="w-32 h-32 rounded-full bg-gradient-to-br from-cyan-500 to-fuchsia-600 p-1 shadow-lg shadow-cyan-500/20 active:scale-95 transition-transform group"
                    >
                        <div className="w-full h-full rounded-full bg-slate-950 flex items-center justify-center group-hover:bg-slate-900 transition-colors">
                            <span className="text-sm font-black tracking-tighter">EXECUTE</span>
                        </div>
                    </button>
                    <p className="text-[10px] text-slate-500 italic text-center">
                        Trigger a real request to the governed engine.
                    </p>
                </div>
            </div>
        </div>
    );
}
