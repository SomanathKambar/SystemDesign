import { useEffect, useRef } from 'react';
import type { RateLimitEvent } from '../types';

interface Props {
  currentTime: number;
  events: RateLimitEvent[];
  config: Record<string, string>;
}

export const FixedWindowVisualizer = ({ currentTime, events, config }: Props) => {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const maxRequests = parseInt(config.capacity || '10');
  const windowSizeMs = 1000; // Hardcoded or from config

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const { width, height } = canvas;
    ctx.clearRect(0, 0, width, height);

    const windowStart = Math.floor(currentTime / windowSizeMs) * windowSizeMs;
    const pastEvents = events.filter(e => e.timestampMs <= currentTime);
    
    // Calculate current window count
    const currentWindowEvents = pastEvents.filter(e => 
        e.timestampMs >= windowStart && e.type === 'REQUEST_ALLOWED'
    );
    const count = currentWindowEvents.length;

    // Draw Window Frame
    const rectWidth = 240;
    const rectHeight = 120;
    const x = width / 2 - rectWidth / 2;
    const y = height / 2 - rectHeight / 2;

    ctx.strokeStyle = '#334155';
    ctx.setLineDash([5, 5]);
    ctx.strokeRect(x - 20, y - 20, rectWidth + 40, rectHeight + 40);
    ctx.setLineDash([]);

    // Draw Window Label
    ctx.fillStyle = '#64748b';
    ctx.font = 'bold 10px font-mono';
    ctx.textAlign = 'left';
    ctx.fillText(`WINDOW: ${windowStart}ms - ${windowStart + windowSizeMs}ms`, x - 20, y - 30);

    // Draw Progress Bar / Blocks
    const blockWidth = (rectWidth - (maxRequests - 1) * 4) / maxRequests;
    for (let i = 0; i < maxRequests; i++) {
        const bx = x + i * (blockWidth + 4);
        if (i < count) {
            ctx.fillStyle = '#10b981'; // Filled
            ctx.shadowBlur = 10;
            ctx.shadowColor = '#10b981';
        } else {
            ctx.fillStyle = '#1e293b'; // Empty
            ctx.shadowBlur = 0;
        }
        ctx.fillRect(bx, y + 20, blockWidth, rectHeight - 40);
        ctx.shadowBlur = 0;
    }

    // Counter Text
    ctx.fillStyle = count >= maxRequests ? '#ef4444' : '#f8fafc';
    ctx.font = 'black 48px Inter';
    ctx.textAlign = 'center';
    ctx.fillText(`${count}`, width / 2, y + rectHeight + 60);
    
    ctx.fillStyle = '#64748b';
    ctx.font = 'bold 12px Inter';
    ctx.fillText(`LIMIT: ${maxRequests}`, width / 2, y + rectHeight + 85);

    // Draw "Blocked" overlay if limit exceeded
    const lastBlocked = [...pastEvents].reverse().find(e => 
        e.timestampMs >= windowStart && e.type === 'REQUEST_BLOCKED' && currentTime - e.timestampMs < 200
    );
    if (lastBlocked) {
        ctx.fillStyle = 'rgba(239, 68, 68, 0.2)';
        ctx.fillRect(0, 0, width, height);
        ctx.fillStyle = '#ef4444';
        ctx.font = 'bold 24px Inter';
        ctx.fillText('BLOCKED!', width / 2, height / 2);
    }

  }, [currentTime, events, maxRequests, windowSizeMs]);

  return (
    <canvas 
      ref={canvasRef} 
      width={600} 
      height={400} 
      className="w-full h-full object-contain"
    />
  );
};
