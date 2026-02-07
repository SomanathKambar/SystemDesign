import type { RateLimitEvent, ExperimentMetadata } from '../types';

interface Props {
  events: RateLimitEvent[];
  metadata: ExperimentMetadata;
  currentTime: number;
}

export const TelemetryReport = ({ events, metadata, currentTime }: Props) => {
  const duration = parseInt(metadata.config.duration || '30000');
  const slotDuration = duration / 10;
  const slots = Array.from({ length: 10 }, (_, i) => ({
    start: i * slotDuration,
    end: (i + 1) * slotDuration,
    index: i + 1
  }));

  const getAnalysis = (slotEvents: RateLimitEvent[], strategy: string, slotIndex: number) => {
    const allowed = slotEvents.filter(e => e.type === 'REQUEST_ALLOWED').length;
    const blocked = slotEvents.filter(e => e.type === 'REQUEST_BLOCKED').length;
    const total = allowed + blocked;
    const successRate = total > 0 ? Math.round((allowed / total) * 100) : 0;

    if (total === 0) return "Idle state. No traffic detected (0 requests).";
    
    if (blocked === 0) return `Perfect flow control. All ${allowed} requests were allowed (100% success). The strategy successfully handled the load within limits.`;

    if (strategy === 'FIXED_WINDOW') {
        const capacity = parseInt(metadata.config.capacity);
        if (slotIndex > 1 && allowed > capacity) {
            return `CRITICAL: Boundary vulnerability exploited. Allowed ${allowed} requests, exceeding capacity of ${capacity} (Success Rate: ${successRate}%). This occurs when a burst spans the window reset boundary.`;
        }
        return `Window saturation. Capacity reached. ${blocked} requests blocked (${100 - successRate}% rejection) to enforce the limit of ${capacity}.`;
    }

    if (strategy === 'TOKEN_BUCKET') {
        if (allowed > 0 && blocked > 0) return `Throttling active. Allowed ${allowed} but blocked ${blocked} (${successRate}% success). Bucket emptied due to sustained burst, enforcing token refill rate.`;
        return `Overflow protection. ${blocked} requests rejected to prevent system overload.`;
    }

    if (strategy === 'LEAKY_BUCKET') {
        return `Queue shaping active. ${allowed} requests processed, ${blocked} dropped (${successRate}% success). The constant leak rate caused the queue (bucket) to fill up during this burst.`;
    }

    return `Dynamic limiting. ${allowed} allowed, ${blocked} blocked (${successRate}% success). Sliding window calculated rate limit exceeded, smoothing traffic spikes.`;
  };

  return (
    <div className="space-y-4 overflow-y-auto custom-scrollbar pr-2 h-[400px]">
      {slots.map((slot) => {
        const isPast = currentTime >= slot.start;
        const isCurrent = currentTime >= slot.start && currentTime < slot.end;
        const slotEvents = events.filter(e => e.timestampMs >= slot.start && e.timestampMs < slot.end);
        const allowed = slotEvents.filter(e => e.type === 'REQUEST_ALLOWED').length;
        const blocked = slotEvents.filter(e => e.type === 'REQUEST_BLOCKED').length;
        const total = allowed + blocked;
        const successRate = total > 0 ? ((allowed / total) * 100).toFixed(1) : '0.0';

        if (!isPast) return (
            <div key={slot.index} className="p-4 rounded-2xl border border-white/5 bg-slate-900/20 opacity-30">
                <span className="text-[10px] font-black text-slate-600 uppercase tracking-widest">Slot {slot.index} (Waiting...)</span>
            </div>
        );

        return (
          <div 
            key={slot.index} 
            className={`p-4 rounded-2xl border transition-all duration-500 ${
                isCurrent ? 'border-cyan-500 bg-cyan-500/10 shadow-[0_0_15px_rgba(34,211,238,0.1)]' : 'border-slate-800 bg-slate-800/20'
            }`}
          >
            <div className="flex justify-between items-center mb-2">
                <div className="flex items-center gap-3">
                    <span className={`text-[10px] font-black uppercase tracking-widest ${isCurrent ? 'text-cyan-400' : 'text-slate-500'}`}>
                        Slot {slot.index} <span className="text-slate-600">({(slot.start/1000).toFixed(1)}s - {(slot.end/1000).toFixed(1)}s)</span>
                    </span>
                    {total > 0 && (
                        <span className={`text-[9px] px-1.5 py-0.5 rounded font-mono font-bold ${
                            Number(successRate) === 100 ? 'bg-green-500/20 text-green-400' : 
                            Number(successRate) > 50 ? 'bg-amber-500/20 text-amber-400' : 'bg-red-500/20 text-red-400'
                        }`}>
                            {successRate}% Success
                        </span>
                    )}
                </div>
                <div className="flex gap-3 text-[9px] font-mono font-bold">
                    <span className="text-slate-400">Total: {total}</span>
                    <span className="text-green-400">Allowed: {allowed}</span>
                    <span className="text-red-400">Blocked: {blocked}</span>
                </div>
            </div>
            <p className="text-[11px] text-slate-300 leading-relaxed font-medium border-t border-white/5 pt-2 mt-2">
                {getAnalysis(slotEvents, metadata.strategy, slot.index)}
            </p>
          </div>
        );
      })}
    </div>
  );
};
