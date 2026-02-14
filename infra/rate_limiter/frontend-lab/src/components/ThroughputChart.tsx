import { useEffect, useRef } from 'react';
import type { RateLimitEvent } from '../types';

interface Props {
    events: RateLimitEvent[];
    windowSizeMs: number;
    limit: number;
}

export const ThroughputChart = ({ events, windowSizeMs, limit }: Props) => {
    const canvasRef = useRef<HTMLCanvasElement>(null);

    useEffect(() => {
        const canvas = canvasRef.current;
        if (!canvas) return;
        const ctx = canvas.getContext('2d');
        if (!ctx) return;

        const { width, height } = canvas;
        ctx.clearRect(0, 0, width, height);

        if (events.length === 0) return;

        const now = Date.now();
        const duration = 30000; // 30 seconds view
        const startTime = now - duration;

        // Calculate throughput points
        // Group events into 1-second buckets
        const bucketSize = 1000;
        const buckets: Record<number, number> = {};
        
        events.forEach(e => {
            if (e.timestampMs >= startTime && e.type === 'REQUEST_ALLOWED') {
                const bucket = Math.floor(e.timestampMs / bucketSize) * bucketSize;
                buckets[bucket] = (buckets[bucket] || 0) + 1;
            }
        });

        const points: { x: number, y: number }[] = [];
        for (let t = startTime; t <= now; t += bucketSize) {
            const bucket = Math.floor(t / bucketSize) * bucketSize;
            const count = buckets[bucket] || 0;
            
            const x = ((t - startTime) / duration) * width;
            const y = height - (count / (limit * 2)) * height; // Scale by 2x limit to show headroom
            points.push({ x, y });
        }

        // Draw background grid
        ctx.strokeStyle = 'rgba(255, 255, 255, 0.05)';
        ctx.lineWidth = 1;
        for (let i = 0; i <= 4; i++) {
            const gy = (i / 4) * height;
            ctx.beginPath();
            ctx.moveTo(0, gy);
            ctx.lineTo(width, gy);
            ctx.stroke();
        }

        // Draw Limit Line
        const limitY = height - (limit / (limit * 2)) * height;
        ctx.strokeStyle = 'rgba(239, 68, 68, 0.3)';
        ctx.setLineDash([5, 5]);
        ctx.beginPath();
        ctx.moveTo(0, limitY);
        ctx.lineTo(width, limitY);
        ctx.stroke();
        ctx.setLineDash([]);
        
        ctx.fillStyle = 'rgba(239, 68, 68, 0.5)';
        ctx.font = '10px font-mono';
        ctx.fillText('LIMIT', 5, limitY - 5);

        // Draw Line
        if (points.length > 1) {
            ctx.strokeStyle = '#22d3ee';
            ctx.lineWidth = 2;
            ctx.lineJoin = 'round';
            ctx.beginPath();
            ctx.moveTo(points[0].x, points[0].y);
            for (let i = 1; i < points.length; i++) {
                ctx.lineTo(points[i].x, points[i].y);
            }
            ctx.stroke();

            // Fill Area
            ctx.lineTo(points[points.length - 1].x, height);
            ctx.lineTo(points[0].x, height);
            const gradient = ctx.createLinearGradient(0, 0, 0, height);
            gradient.addColorStop(0, 'rgba(34, 211, 238, 0.2)');
            gradient.addColorStop(1, 'rgba(34, 211, 238, 0)');
            ctx.fillStyle = gradient;
            ctx.fill();
        }

    }, [events, limit, windowSizeMs]);

    return (
        <div className="relative w-full h-24 bg-slate-950/50 rounded-xl border border-white/5 overflow-hidden">
            <div className="absolute top-2 left-3 text-[10px] font-mono text-slate-500 uppercase tracking-widest">Throughput (Req/s)</div>
            <canvas ref={canvasRef} width={400} height={100} className="w-full h-full" />
        </div>
    );
};
