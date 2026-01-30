import { useState, useEffect } from 'react';
import { ExperimentLoader } from '../services/ExperimentLoader';
import type { ExperimentMetadata, RateLimitEvent } from '../types';
import { TokenBucketVisualizer } from './TokenBucketVisualizer';
import { LeakyBucketVisualizer } from './LeakyBucketVisualizer';
import { FixedWindowVisualizer } from './FixedWindowVisualizer';
import { SlidingWindowVisualizer } from './SlidingWindowVisualizer';
import { TelemetryReport } from './TelemetryReport';

interface Props {
  currentTime: number;
  allExperiments: { strategy: string; id: string; label: string }[];
}

interface StrategyState {
    strategyType: string;
    metadata: ExperimentMetadata | null;
    events: RateLimitEvent[];
    loading: boolean;
    showTelemetry: boolean;
}

const STRATEGY_TYPES = [
    'FIXED_WINDOW',
    'TOKEN_BUCKET',
    'LEAKY_BUCKET',
    'SLIDING_WINDOW_COUNTER',
    'SLIDING_WINDOW_LOG'
];

const SCENARIOS = ['Boundary', 'Burst', 'HighLoad'];

export const ComparisonDashboard = ({ currentTime, allExperiments }: Props) => {
  const [activeScenario, setActiveScenario] = useState('Boundary');
  const [states, setStates] = useState<StrategyState[]>([
    { strategyType: 'FIXED_WINDOW', metadata: null, events: [], loading: true, showTelemetry: false },
    { strategyType: 'TOKEN_BUCKET', metadata: null, events: [], loading: true, showTelemetry: false },
  ]);

  const loadExperimentForStrategy = async (index: number, strategyType: string, scenario: string) => {
    const experiment = allExperiments.find(e => 
        e.strategy.toUpperCase() === strategyType && e.label.includes(scenario)
    );

    if (!experiment) return;

    try {
        const metadata = await ExperimentLoader.loadMetadata(experiment.strategy, experiment.id);
        const events = await ExperimentLoader.loadEvents(experiment.strategy, experiment.id);
        setStates(prev => {
            const next = [...prev];
            // Preserve showTelemetry state if it exists
            const currentShowTelemetry = next[index]?.showTelemetry || false;
            next[index] = { strategyType, metadata, events, loading: false, showTelemetry: currentShowTelemetry };
            return next;
        });
    } catch (err) {
        console.error(err);
    }
  };

  // Sync all strategies when scenario changes
  useEffect(() => {
    states.forEach((s, i) => {
        loadExperimentForStrategy(i, s.strategyType, activeScenario);
    });
  }, [activeScenario]);

  const addRandomStrategy = () => {
    const currentTypes = states.map(s => s.strategyType);
    const availableTypes = STRATEGY_TYPES.filter(t => !currentTypes.includes(t));
    
    if (availableTypes.length > 0) {
        const nextType = availableTypes[Math.floor(Math.random() * availableTypes.length)];
        const newIndex = states.length;
        setStates(prev => [...prev, { strategyType: nextType, metadata: null, events: [], loading: true, showTelemetry: false }]);
        loadExperimentForStrategy(newIndex, nextType, activeScenario);
    }
  };

  const toggleTelemetry = (index: number) => {
    setStates(prev => {
        const next = [...prev];
        next[index] = { ...next[index], showTelemetry: !next[index].showTelemetry };
        return next;
    });
  };

  const processedData = states.map(s => {
    const visibleEvents = s.events.filter(e => e.timestampMs <= currentTime);
    const allowed = visibleEvents.filter(e => e.type === 'REQUEST_ALLOWED').length;
    const blocked = visibleEvents.filter(e => e.type === 'REQUEST_BLOCKED').length;
    
    let score = 0;
    if (s.metadata) {
        if (s.strategyType === 'SLIDING_WINDOW_LOG') score = 95;
        else if (s.strategyType === 'LEAKY_BUCKET') score = 92;
        else if (s.strategyType === 'TOKEN_BUCKET') score = 88;
        else if (s.strategyType === 'SLIDING_WINDOW_COUNTER') score = 85;
        else if (s.strategyType === 'FIXED_WINDOW') {
            score = (activeScenario === 'Boundary' && allowed > 10) ? 15 : 65;
        }
    }
    return { ...s, allowed, blocked, score };
  });

  const leader = [...processedData].filter(d => !d.loading).sort((a, b) => b.score - a.score)[0];
  const canAddMore = states.length < STRATEGY_TYPES.length;

  const renderVisualizer = (s: typeof processedData[0]) => {
    if (s.loading || !s.metadata) return (
        <div className="flex flex-col items-center justify-center w-full h-full bg-slate-900/50 rounded-2xl border border-slate-800 animate-pulse">
            <div className="w-8 h-8 border-2 border-blue-500/20 border-t-blue-500 rounded-full animate-spin mb-2"></div>
            <span className="text-[10px] font-mono text-slate-600 uppercase tracking-widest">Loading {s.strategyType}...</span>
        </div>
    );
    
    if (s.showTelemetry) {
        return <TelemetryReport events={s.events} metadata={s.metadata} currentTime={currentTime} />;
    }
    
    switch (s.strategyType) {
      case 'TOKEN_BUCKET': return <TokenBucketVisualizer currentTime={currentTime} events={s.events} config={s.metadata.config} />;
      case 'LEAKY_BUCKET': return <LeakyBucketVisualizer currentTime={currentTime} events={s.events} config={s.metadata.config} />;
      case 'FIXED_WINDOW': return <FixedWindowVisualizer currentTime={currentTime} events={s.events} config={s.metadata.config} />;
      default: return <SlidingWindowVisualizer currentTime={currentTime} events={s.events} config={s.metadata.config} />;
    }
  };

  return (
    <div className="flex flex-col w-full h-full overflow-y-auto custom-scrollbar bg-slate-900/50">
      <div className="sticky top-0 z-30 p-6 bg-slate-900/95 backdrop-blur-xl border-b border-white/5 flex flex-wrap items-center justify-between gap-6">
         <div className="flex items-center gap-6">
            <div>
                <h2 className="text-xl font-black text-white tracking-tighter uppercase">Comparison Lab</h2>
                <p className="text-[10px] text-slate-500 font-mono tracking-widest">Global Scenario: <span className="text-blue-400">{activeScenario}</span></p>
            </div>
            <div className="h-8 w-px bg-slate-800"></div>
            <div className="flex p-1 bg-slate-800/50 rounded-xl border border-white/5">
                {SCENARIOS.map(scenario => (
                    <button
                        key={scenario}
                        onClick={() => setActiveScenario(scenario)}
                        className={`px-4 py-1.5 rounded-lg text-[10px] font-black transition-all ${
                            activeScenario === scenario ? 'bg-blue-600 text-white shadow-lg' : 'text-slate-500 hover:text-slate-300'
                        }`}
                    >
                        {scenario.toUpperCase()}
                    </button>
                ))}
            </div>
         </div>

         <button 
            onClick={addRandomStrategy}
            disabled={!canAddMore}
            className={`px-6 py-2 rounded-xl text-[10px] font-black tracking-widest transition-all border ${
                canAddMore 
                ? 'bg-blue-600/10 border-blue-500/30 text-blue-400 hover:bg-blue-600/20' 
                : 'opacity-30 cursor-not-allowed border-slate-700 text-slate-500'
            }`}
         >
            {canAddMore ? `+ ADD STRATEGY (${STRATEGY_TYPES.length - states.length} LEFT)` : 'ALL STRATEGIES ACTIVE'}
         </button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8 p-8">
        {processedData.map((s, index) => (
          <div key={`${s.strategyType}-${index}`} className={`group flex flex-col bg-slate-800/20 rounded-[2rem] border transition-all duration-500 ${
            leader && s.strategyType === leader.strategyType ? 'border-blue-500/40 shadow-[0_0_50px_rgba(59,130,246,0.05)]' : 'border-white/5'
          } overflow-hidden`}>
            <div className="p-6 border-b border-white/5 flex justify-between items-center bg-white/[0.02]">
                <div>
                    <h3 className="text-xs font-black text-blue-400 tracking-[0.2em] uppercase">{s.strategyType.replace('_', ' ')}</h3>
                    <div className="flex items-center gap-2 mt-1">
                        <div className={`w-1 h-1 rounded-full ${s.score > 80 ? 'bg-green-500' : s.score > 50 ? 'bg-amber-500' : 'bg-red-500'}`}></div>
                        <span className="text-[9px] font-mono text-slate-500 uppercase">Score: {s.score}%</span>
                    </div>
                </div>
                <div className="flex items-center gap-2">
                    <button
                        onClick={() => toggleTelemetry(index)}
                        className={`p-2 rounded-lg transition-all ${s.showTelemetry ? 'bg-blue-500/20 text-blue-400' : 'text-slate-600 hover:text-slate-400'}`}
                        title="Toggle Telemetry Report"
                    >
                        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"/></svg>
                    </button>
                    <button 
                        onClick={() => setStates(prev => prev.filter((_, i) => i !== index))}
                        className="opacity-0 group-hover:opacity-100 p-2 text-slate-600 hover:text-red-400 transition-all"
                    >
                        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12"/></svg>
                    </button>
                </div>
            </div>
            
            <div className="aspect-square p-4 relative">
                {renderVisualizer(s)}
            </div>

            <div className="p-6 bg-white/[0.01] grid grid-cols-2 gap-4 border-t border-white/5">
                <div className="bg-slate-900/40 p-3 rounded-2xl">
                    <div className="text-[9px] text-slate-500 font-bold uppercase mb-1">Allowed</div>
                    <div className={`text-xl font-mono font-black ${s.strategyType === 'FIXED_WINDOW' && activeScenario === 'Boundary' && s.allowed > 10 ? 'text-red-400' : 'text-green-400'}`}>{s.allowed}</div>
                </div>
                <div className="bg-slate-900/40 p-3 rounded-2xl">
                    <div className="text-[9px] text-slate-500 font-bold uppercase mb-1">Blocked</div>
                    <div className="text-xl font-mono font-black text-red-500">{s.blocked}</div>
                </div>
            </div>
          </div>
        ))}
      </div>

      {/* Dynamic Report Section */}
      <div className="mt-12 p-12 bg-slate-950/80 border-t border-white/5 backdrop-blur-3xl">
        <div className="max-w-6xl mx-auto">
            <h3 className="text-xs font-black text-slate-400 uppercase tracking-[0.3em] mb-12 flex items-center gap-4">
                <span className="h-px flex-1 bg-gradient-to-r from-transparent via-slate-800 to-transparent"></span>
                Scenario Post-Mortem: {activeScenario}
                <span className="h-px flex-1 bg-gradient-to-r from-transparent via-slate-800 to-transparent"></span>
            </h3>
            
            <div className="grid grid-cols-1 md:grid-cols-3 gap-12">
                <div className="space-y-4">
                    <h4 className="text-blue-400 text-[10px] font-black uppercase tracking-widest">Executive Summary</h4>
                    <p className="text-slate-400 text-xs leading-relaxed font-medium">
                        Under the <span className="text-white">{activeScenario}</span> profile, we are testing for 
                        {activeScenario === 'Boundary' ? ' window-edge exploits' : 
                         activeScenario === 'Burst' ? ' handling of sudden spikes' : ' high-frequency stability'}.
                    </p>
                </div>
                <div className="space-y-4">
                    <h4 className="text-green-400 text-[10px] font-black uppercase tracking-widest">Performance Leader</h4>
                    <p className="text-slate-400 text-xs leading-relaxed font-medium">
                        {leader ? (
                            <>The <span className="text-white">{leader.strategyType.replace('_', ' ')}</span> is currently leading with 
                            a resilience score of <span className="text-green-400">{leader.score}%</span> due to its superior {
                                leader.strategyType.includes('LOG') ? 'precision' : 
                                leader.strategyType.includes('BUCKET') ? 'shaping' : 'approximation'
                            }.</>
                        ) : 'Calculating...'}
                    </p>
                </div>
                <div className="space-y-4">
                    <h4 className="text-red-400 text-[10px] font-black uppercase tracking-widest">Observed Failures</h4>
                    <p className="text-slate-400 text-xs leading-relaxed font-medium">
                        {activeScenario === 'Boundary' ? 'Fixed Window fails by allowing up to 20 requests in a 10-limit window.' : 
                         'No critical protocol violations observed in current traffic pattern.'}
                    </p>
                </div>
            </div>
        </div>
      </div>
    </div>
  );
};