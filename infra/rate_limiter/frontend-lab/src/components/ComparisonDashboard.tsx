import { useState, useEffect } from 'react';
import { ExperimentLoader } from '../services/ExperimentLoader';
import type { ExperimentMetadata, RateLimitEvent } from '../types';
import { TokenBucketVisualizer } from './TokenBucketVisualizer';
import { FixedWindowVisualizer } from './FixedWindowVisualizer';
import { SlidingWindowVisualizer } from './SlidingWindowVisualizer';

interface Props {
  currentTime: number;
  selectedExperiments: { strategy: string; id: string; label: string }[];
}

export const ComparisonDashboard = ({ currentTime, selectedExperiments }: Props) => {
  const [data, setData] = useState<{
    metadata: ExperimentMetadata;
    events: RateLimitEvent[];
  }[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    Promise.all(
      selectedExperiments.map(async (exp) => {
        const metadata = await ExperimentLoader.loadMetadata(exp.strategy, exp.id);
        const events = await ExperimentLoader.loadEvents(exp.strategy, exp.id);
        return { metadata, events };
      })
    ).then((results) => {
      setData(results);
      setLoading(false);
    });
  }, [selectedExperiments]);

  if (loading) {
    return (
      <div className="h-full flex flex-col items-center justify-center">
        <div className="w-12 h-12 border-4 border-blue-500/30 border-t-blue-500 rounded-full animate-spin mb-4"></div>
        <div className="text-[10px] font-mono uppercase tracking-widest text-slate-500">Synchronizing Simulations...</div>
      </div>
    );
  }

  const renderVisualizer = (metadata: ExperimentMetadata, events: RateLimitEvent[]) => {
    switch (metadata.strategy) {
      case 'TOKEN_BUCKET':
        return <TokenBucketVisualizer currentTime={currentTime} events={events} config={metadata.config} />
      case 'FIXED_WINDOW':
        return <FixedWindowVisualizer currentTime={currentTime} events={events} config={metadata.config} />
      case 'SLIDING_WINDOW_COUNTER':
      case 'SLIDING_WINDOW_LOG':
        return <SlidingWindowVisualizer currentTime={currentTime} events={events} config={metadata.config} />
      default:
        return null;
    }
  };

  // Determine Winner
  const strategyPrecedence = ['SLIDING_WINDOW_LOG', 'SLIDING_WINDOW_COUNTER', 'TOKEN_BUCKET', 'FIXED_WINDOW'];
  const processedData = data.map(item => {
    const visibleEvents = item.events.filter(e => e.timestampMs <= currentTime);
    return {
        ...item,
        allowed: visibleEvents.filter(e => e.type === 'REQUEST_ALLOWED').length,
        blocked: visibleEvents.filter(e => e.type === 'REQUEST_BLOCKED').length,
    };
  });

  const winner = [...processedData].sort((a, b) => {
    if (b.allowed !== a.allowed) return b.allowed - a.allowed;
    return strategyPrecedence.indexOf(a.metadata.strategy) - strategyPrecedence.indexOf(b.metadata.strategy);
  })[0];

  return (
    <div className="flex flex-col w-full h-full">
      <div className="p-6 bg-blue-600/10 border-b border-blue-500/20 mb-4 flex items-center justify-between">
         <div>
            <h2 className="text-lg font-bold text-blue-400">SYNCED COMPARISON</h2>
            <p className="text-[10px] text-slate-500 uppercase tracking-widest">Evaluating {data.length} strategies under identical traffic</p>
         </div>
         {winner && (
            <div className="text-right">
                <span className="text-[10px] text-slate-500 uppercase block mb-1">Current Leader</span>
                <span className="bg-blue-500 text-white text-[10px] font-black px-3 py-1 rounded-full shadow-[0_0_15px_rgba(59,130,246,0.5)]">
                    {winner.metadata.strategy}
                </span>
            </div>
         )}
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6 w-full flex-1 p-4 overflow-y-auto">
        {processedData.map((item, index) => {
          return (
            <div key={`${item.metadata.strategy}-${index}`} className={`flex flex-col bg-slate-900/40 rounded-2xl border transition-all duration-300 ${
                item.metadata.id === winner.metadata.id ? 'border-blue-500/50 shadow-[0_0_30px_rgba(59,130,246,0.1)]' : 'border-slate-700/50'
            } overflow-hidden`}>
              <div className="p-4 border-b border-slate-700/50 flex justify-between items-center bg-slate-800/30">
                <h3 className="text-xs font-bold text-blue-400 tracking-wider uppercase">{item.metadata.strategy.replace('_', ' ')}</h3>
                <div className="flex gap-4">
                  <div className="text-[10px] font-mono">
                     <span className="text-slate-500 uppercase mr-2">Allowed</span>
                     <span className="text-green-400 font-bold">{item.allowed}</span>
                  </div>
                  <div className="text-[10px] font-mono">
                     <span className="text-slate-500 uppercase mr-2">Blocked</span>
                     <span className="text-red-400 font-bold">{item.blocked}</span>
                  </div>
                </div>
              </div>
              
              <div className="flex-1 aspect-video relative">
                {renderVisualizer(item.metadata, item.events)}
              </div>
              
              <div className="p-3 bg-slate-900/60 flex items-center justify-between">
                  <p className="text-[9px] text-slate-500 italic truncate pr-4">{item.metadata.description}</p>
                  <div className="flex items-center gap-2">
                      {item.metadata.id === winner.metadata.id && <span className="text-[8px] bg-green-500/20 text-green-400 px-2 py-0.5 rounded font-bold mr-2 uppercase tracking-tighter">Leading</span>}
                      <div className="w-1.5 h-1.5 rounded-full bg-blue-500 animate-pulse"></div>
                  </div>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};
