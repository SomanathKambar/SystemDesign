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
  const windowSizeMs = parseInt(config.windowSizeMs || '1000');

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
    // Important: Only count requests that happened WITHIN the CURRENT window
    const currentWindowEvents = pastEvents.filter(e => 
        e.timestampMs >= windowStart && e.type === 'REQUEST_ALLOWED'
    );
    const count = currentWindowEvents.length;

    // Draw Window Frame
    const rectWidth = width * 0.8;
    const rectHeight = 100;
    const x = width / 2 - rectWidth / 2;
    const y = height / 2 - 20;

    // Draw Window Label
    ctx.fillStyle = '#94a3b8'; // Slate-400
    ctx.font = 'bold 12px font-mono';
    ctx.textAlign = 'center';
    ctx.fillText(`CURRENT WINDOW: ${windowStart}ms - ${windowStart + windowSizeMs}ms`, width / 2, y - 40);

    // Draw Progress Bar / Blocks
    const gap = 4;
    const blockWidth = (rectWidth - (maxRequests - 1) * gap) / maxRequests;
    
    for (let i = 0; i < maxRequests; i++) {
        const bx = x + i * (blockWidth + gap);
        if (i < count) {
            ctx.fillStyle = '#10b981'; // Filled
            ctx.shadowBlur = 15;
            ctx.shadowColor = '#10b981';
        } else {
            ctx.fillStyle = 'rgba(255, 255, 255, 0.05)'; // Empty glass
            ctx.shadowBlur = 0;
        }
        
        // Use standard rect for better compatibility
        ctx.beginPath();
        ctx.rect(bx, y, blockWidth, rectHeight);
        ctx.fill();
        ctx.shadowBlur = 0;
    }

    // Counter Text
    ctx.fillStyle = count >= maxRequests ? '#ef4444' : '#f8fafc';
    ctx.font = '900 64px Inter';
    ctx.textAlign = 'center';
    ctx.fillText(`${count}`, width / 2, y - 80);
    
    ctx.fillStyle = '#94a3b8';
    ctx.font = 'bold 14px Inter';
    ctx.fillText(`LIMIT: ${maxRequests}`, width / 2, y + rectHeight + 30);

    // Indicators for recent events
    const recentEvents = pastEvents.filter(e => currentTime - e.timestampMs < 400);
    recentEvents.forEach((e) => {
        const age = currentTime - e.timestampMs;
        const opacity = 1 - (age / 400);
        const yOffset = (age / 400) * 50;
        
        ctx.save();
        ctx.globalAlpha = opacity;
        if (e.type === 'REQUEST_ALLOWED') {
            ctx.fillStyle = '#10b981';
            ctx.font = 'bold 12px Inter';
            ctx.fillText('INCOMING ✓', width / 2, y - 150 - yOffset);
        } else if (e.type === 'REQUEST_BLOCKED') {
            ctx.fillStyle = '#ef4444';
            ctx.font = 'bold 16px Inter';
            ctx.fillText('BLOCKED ✕', width / 2, y + rectHeight + 80 + yOffset);
            
            // Red flash on frame
            ctx.strokeStyle = '#ef4444';
            ctx.lineWidth = 2;
            ctx.strokeRect(x - 10, y - 10, rectWidth + 20, rectHeight + 20);
        }
        ctx.restore();
    });

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
