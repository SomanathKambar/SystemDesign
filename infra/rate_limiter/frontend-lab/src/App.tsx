import { useEffect, useState, useRef } from 'react'
import { ExperimentLoader } from './services/ExperimentLoader'
import type { ExperimentMetadata, RateLimitEvent } from './types'
import { TokenBucketVisualizer } from './components/TokenBucketVisualizer'
import { LeakyBucketVisualizer } from './components/LeakyBucketVisualizer'
import { FixedWindowVisualizer } from './components/FixedWindowVisualizer'
import { SlidingWindowVisualizer } from './components/SlidingWindowVisualizer'
import { ComparisonDashboard } from './components/ComparisonDashboard'

const SAMPLE_EXPERIMENTS = [
    { strategy: 'fixed_window', id: 'fixed_window_boundary', label: 'Fixed Window (Boundary Burst)' },
    { strategy: 'sliding_window_counter', id: 'sliding_window_counter_boundary', label: 'Sliding Counter (Boundary Burst)' },
    { strategy: 'sliding_window_log', id: 'sliding_window_log_boundary', label: 'Sliding Log (Boundary Burst)' },
    { strategy: 'token_bucket', id: 'token_bucket_boundary', label: 'Token Bucket (Boundary Burst)' },
    { strategy: 'leaky_bucket', id: 'leaky_bucket_boundary', label: 'Leaky Bucket (Boundary Burst)' },
    { strategy: 'fixed_window', id: 'fixed_window_highload', label: 'Fixed Window (High Load)' },
    { strategy: 'sliding_window_counter', id: 'sliding_window_counter_highload', label: 'Sliding Counter (High Load)' },
    { strategy: 'sliding_window_log', id: 'sliding_window_log_highload', label: 'Sliding Log (High Load)' },
    { strategy: 'token_bucket', id: 'token_bucket_highload', label: 'Token Bucket (High Load)' },
    { strategy: 'leaky_bucket', id: 'leaky_bucket_highload', label: 'Leaky Bucket (High Load)' },
    { strategy: 'fixed_window', id: 'fixed_window_burst', label: 'Fixed Window (Standard Burst)' },
    { strategy: 'token_bucket', id: 'token_bucket_burst', label: 'Token Bucket (Standard Burst)' },
    { strategy: 'leaky_bucket', id: 'leaky_bucket_burst', label: 'Leaky Bucket (Standard Burst)' },
]

function App() {
  const [selectedExp, setSelectedExp] = useState(SAMPLE_EXPERIMENTS[0])
  const [viewMode, setViewMode] = useState<'single' | 'comparison'>('single')
  const [metadata, setMetadata] = useState<ExperimentMetadata | null>(null)
  const [events, setEvents] = useState<RateLimitEvent[]>([])
  const [currentTime, setCurrentTime] = useState(0)
  const [isPlaying, setIsPlaying] = useState(false)
  const [playbackSpeed, setPlaybackSpeed] = useState(1)
  
  const requestRef = useRef<number | null>(null)
  const lastUpdateTimeRef = useRef<number | null>(null)

  useEffect(() => {
    if (viewMode === 'comparison') {
        setMetadata(null);
        setEvents([]);
        return;
    }

    setMetadata(null)
    setEvents([])
    setCurrentTime(0)
    setIsPlaying(false)

    ExperimentLoader.loadMetadata(selectedExp.strategy, selectedExp.id)
      .then(setMetadata)
      .catch(console.error)

    ExperimentLoader.loadEvents(selectedExp.strategy, selectedExp.id)
      .then(setEvents)
      .catch(console.error)
  }, [selectedExp, viewMode])

  const animate = (time: number) => {
    if (lastUpdateTimeRef.current !== null) {
      const deltaTime = time - lastUpdateTimeRef.current
      setCurrentTime((prev) => {
        const next = prev + deltaTime * playbackSpeed
        const maxDuration = metadata ? parseInt(metadata.config.duration) : 10000
        if (next >= maxDuration) {
          setIsPlaying(false)
          return maxDuration
        }
        return next
      })
    }
    lastUpdateTimeRef.current = time
    requestRef.current = requestAnimationFrame(animate)
  }

  useEffect(() => {
    if (isPlaying) {
      lastUpdateTimeRef.current = performance.now()
      requestRef.current = requestAnimationFrame(animate)
    } else {
      if (requestRef.current !== null) cancelAnimationFrame(requestRef.current)
    }
    return () => {
      if (requestRef.current !== null) cancelAnimationFrame(requestRef.current)
    }
  }, [isPlaying, playbackSpeed, metadata])

  const visibleEvents = events.filter(e => e.timestampMs <= currentTime)
  const latestEvents = visibleEvents.slice(-8).reverse()

  const renderVisualizer = () => {
    if (!metadata) return null;

    switch (metadata.strategy) {
      case 'TOKEN_BUCKET':
        return <TokenBucketVisualizer currentTime={currentTime} events={events} config={metadata.config} />
      case 'LEAKY_BUCKET':
        return <LeakyBucketVisualizer currentTime={currentTime} events={events} config={metadata.config} />
      case 'FIXED_WINDOW':
        return <FixedWindowVisualizer currentTime={currentTime} events={events} config={metadata.config} />
      case 'SLIDING_WINDOW_COUNTER':
      case 'SLIDING_WINDOW_LOG':
        return <SlidingWindowVisualizer currentTime={currentTime} events={events} config={metadata.config} />
      default:
        return (
          <div className="text-center">
            <div className="text-6xl mb-4">⚙️</div>
            <div className="text-slate-500 font-mono uppercase text-xs tracking-widest">Visualizer for {metadata.strategy} Coming Soon</div>
          </div>
        )
    }
  }

  return (
    <div className="min-h-screen bg-[#0B1120] text-white p-8 font-sans selection:bg-cyan-500/30 overflow-y-auto custom-scrollbar">
      <header className="max-w-7xl mx-auto mb-12 flex justify-between items-end">
        <div>
            <h1 className="text-5xl font-black tracking-tighter text-transparent bg-clip-text bg-gradient-to-r from-fuchsia-400 to-cyan-400 mb-2">
                RATE LIMITER LAB
            </h1>
            <div className="flex items-center gap-4">
                <div className="flex p-1 bg-slate-800/50 rounded-lg border border-white/5 mr-2">
                    <button 
                        onClick={() => setViewMode('single')}
                        className={`px-3 py-1 text-[10px] font-bold rounded-md transition-all ${viewMode === 'single' ? 'bg-cyan-600 text-white shadow-lg' : 'text-slate-400 hover:text-slate-200'}`}
                    >
                        SINGLE VIEW
                    </button>
                    <button 
                        onClick={() => setViewMode('comparison')}
                        className={`px-3 py-1 text-[10px] font-bold rounded-md transition-all ${viewMode === 'comparison' ? 'bg-cyan-600 text-white shadow-lg' : 'text-slate-400 hover:text-slate-200'}`}
                    >
                        COMPARISON
                    </button>
                </div>

                {viewMode === 'single' && (
                    <select 
                        value={`${selectedExp.strategy}:${selectedExp.id}`}
                        onChange={(e) => {
                            const [strategy, id] = e.target.value.split(':')
                            const found = SAMPLE_EXPERIMENTS.find(exp => exp.id === id && exp.strategy === strategy);
                            if (found) setSelectedExp(found);
                        }}
                        className="bg-slate-800/50 border border-white/10 text-slate-300 text-xs font-bold py-1 px-3 rounded-lg outline-none focus:border-cyan-500 transition cursor-pointer"
                    >
                        {SAMPLE_EXPERIMENTS.map(exp => (
                            <option key={exp.id} value={`${exp.strategy}:${exp.id}`}>{exp.label}</option>
                        ))}
                    </select>
                )}
                
                {metadata && viewMode === 'single' && (
                    <div className="flex items-center gap-3 border-l border-white/10 pl-4">
                        <p className="text-slate-400 text-sm italic">{metadata.description}</p>
                    </div>
                )}

                {viewMode === 'comparison' && (
                    <div className="flex items-center gap-3 border-l border-white/10 pl-4">
                        <p className="text-slate-400 text-sm italic font-mono uppercase tracking-widest opacity-60">Comparative Analysis: Multi-Strategy Sync</p>
                    </div>
                )}
            </div>
        </div>
        <div className="text-right hidden md:block">
            <div className="text-slate-500 text-[10px] font-mono uppercase tracking-tighter mb-1">System Status</div>
            <div className="text-green-400 font-mono text-xs tracking-widest flex items-center justify-end gap-2">
                <span className="w-1.5 h-1.5 bg-green-500 rounded-full animate-pulse"></span>
                DETERMINISTIC_ENGINE_ACTIVE
            </div>
        </div>
      </header>

      <main className="max-w-7xl mx-auto grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Visualization Area */}
        <div className={viewMode === 'single' ? 'lg:col-span-2 space-y-6' : 'lg:col-span-3 space-y-6'}>
          <div className={`${viewMode === 'single' ? 'aspect-video' : 'h-[600px]'} bg-slate-900/50 backdrop-blur-sm rounded-3xl border border-white/5 shadow-2xl flex items-center justify-center relative overflow-hidden group`}>
             <div className="absolute top-6 left-6 flex items-center gap-2 z-10">
                <div className="w-2 h-2 bg-cyan-500 rounded-full animate-pulse shadow-[0_0_8px_#22d3ee]"></div>
                <span className="text-[10px] font-mono text-cyan-500 uppercase tracking-widest">
                    {viewMode === 'single' ? 'Real-time Simulation' : 'Comparison Matrix'}
                </span>
             </div>
             
             {viewMode === 'single' ? renderVisualizer() : <ComparisonDashboard currentTime={currentTime} allExperiments={SAMPLE_EXPERIMENTS} />}

             {/* Time HUD */}
             <div className="absolute bottom-6 right-8 text-right z-10 bg-slate-950/80 px-4 py-2 rounded-2xl backdrop-blur-md border border-white/5">
                <div className="text-[10px] font-mono text-slate-500 uppercase tracking-widest mb-1">Elapsed Time</div>
                <div className="text-3xl font-mono font-bold tabular-nums text-cyan-400">
                    {currentTime.toFixed(0).padStart(5, '0')}<span className="text-cyan-900 ml-1">MS</span>
                </div>
             </div>
          </div>

          {/* Controls */}
          <div className="bg-slate-900/50 backdrop-blur-md p-8 rounded-3xl border border-white/5 shadow-xl">
            <div className="flex flex-wrap items-center gap-6 mb-8">
              <button 
                onClick={() => setIsPlaying(!isPlaying)}
                className={`group relative flex items-center justify-center w-14 h-14 rounded-full transition-all duration-300 ${
                    isPlaying ? 'bg-fuchsia-500 hover:bg-fuchsia-400' : 'bg-cyan-600 hover:bg-cyan-500'
                } shadow-lg shadow-cyan-900/20`}
              >
                {isPlaying ? (
                    <svg className="w-6 h-6 fill-white" viewBox="0 0 24 24"><path d="M6 19h4V5H6v14zm8-14v14h4V5h-4z"/></svg>
                ) : (
                    <svg className="w-6 h-6 fill-white ml-1" viewBox="0 0 24 24"><path d="M8 5v14l11-7z"/></svg>
                )}
              </button>
              
              <button 
                onClick={() => { setCurrentTime(0); setIsPlaying(false); }}
                className="p-3 rounded-xl bg-white/5 hover:bg-white/10 text-slate-300 transition-colors"
                title="Reset Simulation"
              >
                <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"/></svg>
              </button>

              <div className="h-8 w-px bg-white/5 mx-2"></div>

              <div className="flex items-center gap-3">
                <span className="text-[10px] font-mono text-slate-500 uppercase tracking-widest">Playback Speed</span>
                <div className="flex p-1 bg-slate-950/50 rounded-xl border border-white/5">
                    {[0.5, 1, 2, 5].map(speed => (
                    <button
                        key={speed}
                        onClick={() => setPlaybackSpeed(speed)}
                        className={`px-4 py-1.5 rounded-lg text-xs font-bold transition-all ${
                            playbackSpeed === speed ? 'bg-cyan-600 text-white shadow-md' : 'text-slate-500 hover:text-slate-300'
                        }`}
                    >
                        {speed}x
                    </button>
                    ))}
                </div>
              </div>
            </div>
            
            <div className="relative pt-2">
                <input 
                    type="range" 
                    min="0" 
                    max={metadata ? metadata.config.duration : 10000} 
                    value={currentTime}
                    onChange={(e) => setCurrentTime(parseInt(e.target.value))}
                    className="w-full h-1.5 bg-slate-800 rounded-lg appearance-none cursor-pointer accent-cyan-500"
                />
                <div className="flex justify-between mt-2 text-[10px] font-mono text-slate-600 uppercase tracking-tighter">
                    <span>00000ms</span>
                    <span>{metadata?.config.duration || '10000'}ms</span>
                </div>
            </div>
          </div>
        </div>

        {/* Sidebar / Logs (Only in single view) */}
        {viewMode === 'single' && (
          <div className="space-y-6">
            <div className="bg-slate-900/50 backdrop-blur-md p-6 rounded-3xl border border-white/5 shadow-xl h-[640px] flex flex-col">
              <div className="flex items-center justify-between mb-6">
                  <h3 className="text-sm font-black uppercase tracking-widest text-slate-400 flex items-center gap-2">
                      <span className="w-1.5 h-1.5 bg-green-500 rounded-full shadow-[0_0_5px_#22c55e]"></span>
                      Telemetry Stream
                  </h3>
                  <span className="text-[10px] font-mono text-slate-500">{visibleEvents.length} Events</span>
              </div>
              
              <div className="flex-1 overflow-y-auto pr-2 space-y-3 scrollbar-thin scrollbar-thumb-slate-800 scrollbar-track-transparent">
                {latestEvents.map((event) => (
                  <div key={event.eventId} className={`p-4 rounded-2xl border transition-all duration-500 animate-in slide-in-from-right-8 ${
                    event.type === 'REQUEST_ALLOWED' ? 'border-green-500/20 bg-green-500/5 text-green-400 shadow-[inset_0_0_20px_rgba(34,197,94,0.05)]' :
                    event.type === 'REQUEST_BLOCKED' ? 'border-red-500/20 bg-red-500/5 text-red-400 shadow-[inset_0_0_20px_rgba(239,68,68,0.05)]' :
                    'border-cyan-500/20 bg-cyan-500/5 text-cyan-400'
                  }`}>
                    <div className="flex justify-between items-start mb-2">
                      <span className="text-[10px] font-black tracking-widest uppercase">{event.type.replace('_', ' ')}</span>
                      <span className="text-[9px] font-mono opacity-40">{event.timestampMs.toFixed(0)}ms</span>
                    </div>
                    <div className="grid grid-cols-1 gap-1">
                      {Object.entries(event.payload).map(([k, v]) => (
                        <div key={k} className="flex justify-between text-[10px] font-mono">
                          <span className="opacity-40 uppercase">{k}</span>
                          <span className="opacity-90">{v}</span>
                        </div>
                      ))}
                      {event.type === 'TOKEN_REFILLED' && (
                          <div className="flex justify-between text-[10px] font-mono border-t border-cyan-500/10 mt-1 pt-1">
                              <span className="opacity-40 uppercase">Added</span>
                              <span className="text-cyan-300">+{event.tokensAdded?.toFixed(2)}</span>
                          </div>
                      )}
                    </div>
                  </div>
                ))}
                {!metadata && (
                  <div className="h-full flex flex-col items-center justify-center animate-pulse">
                      <div className="w-12 h-12 border-4 border-cyan-500/30 border-t-cyan-500 rounded-full animate-spin mb-4"></div>
                      <div className="text-[10px] font-mono uppercase tracking-widest text-slate-500">Loading Telemetry...</div>
                  </div>
                )}
              </div>
            </div>
          </div>
        )}
      </main>

      {/* Scenario Catalog (Single View Only) */}
      {viewMode === 'single' && (
          <div className="max-w-7xl mx-auto mt-20 mb-20">
              <h3 className="text-xs font-black text-slate-500 uppercase tracking-[0.4em] mb-8 text-center">Scenario Catalog & Lab Reports</h3>
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                  {SAMPLE_EXPERIMENTS.map((exp) => (
                      <button 
                        key={`${exp.strategy}-${exp.id}`}
                        onClick={() => setSelectedExp(exp)}
                        className={`group p-6 rounded-3xl border transition-all text-left ${
                            selectedExp.id === exp.id 
                            ? 'bg-fuchsia-600/10 border-fuchsia-500/50 shadow-[0_0_30px_rgba(232,121,249,0.1)]' 
                            : 'bg-slate-900/40 border-white/5 hover:border-white/10'
                        }`}
                      >
                          <div className="flex justify-between items-start mb-4">
                              <span className="text-[10px] font-mono text-slate-500 uppercase tracking-widest">{exp.strategy.replace('_', ' ')}</span>
                              {selectedExp.id === exp.id && <span className="w-2 h-2 bg-fuchsia-500 rounded-full animate-pulse"></span>}
                          </div>
                          <h4 className="text-sm font-bold text-slate-200 mb-2">{exp.label}</h4>
                          <p className="text-[10px] text-slate-400 leading-relaxed mb-4">
                              {exp.label.includes('Boundary') ? 'Stress test for window-edge overflow vulnerabilities.' : 
                               exp.label.includes('High Load') ? 'Sustainability test under constant maximum throughput.' : 
                               'Validation of burst handling and recovery speed.'}
                          </p>
                          <div className="flex items-center gap-2">
                              <span className={`text-[9px] font-black uppercase tracking-tighter group-hover:underline ${selectedExp.id === exp.id ? 'text-fuchsia-400' : 'text-cyan-400'}`}>Launch Simulation →</span>
                          </div>
                      </button>
                  ))}
              </div>
          </div>
      )}
    </div>
  )
}

export default App