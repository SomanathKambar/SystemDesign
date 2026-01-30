import { useEffect, useRef } from 'react';
import type { RateLimitEvent } from '../types';

interface Props {
  currentTime: number;
  events: RateLimitEvent[];
  config: Record<string, string>;
}

export const SlidingWindowVisualizer = ({ currentTime, events, config }: Props) => {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const maxRequests = parseInt(config.capacity || '10');
  const windowSizeMs = 1000; 

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const { width, height } = canvas;
    ctx.clearRect(0, 0, width, height);

    const pastEvents = events.filter(e => e.timestampMs <= currentTime);
    const windowStart = currentTime - windowSizeMs;

    // Filter events in current sliding window
    const windowEvents = pastEvents.filter(e => 
        e.timestampMs >= windowStart && e.type === 'REQUEST_ALLOWED'
    );
    const count = windowEvents.length;

    // Draw Main Timeline Axis
    const axisY = height / 2 + 50;
    const axisPadding = 60;
    const axisWidth = width - axisPadding * 2;

    ctx.strokeStyle = '#475569';
    ctx.lineWidth = 2;
    ctx.beginPath();
    ctx.moveTo(axisPadding, axisY);
    ctx.lineTo(width - axisPadding, axisY);
    ctx.stroke();

    // Mapping function: time -> x
    // Let's show a range of [currentTime - 2000, currentTime + 500]
    const viewStart = currentTime - 2000;
    const viewEnd = currentTime + 500;
    const timeToX = (t: number) => {
        const ratio = (t - viewStart) / (viewEnd - viewStart);
        return axisPadding + ratio * axisWidth;
    };

    // Draw Sliding Window Box
    const winXStart = timeToX(windowStart);
    const winXEnd = timeToX(currentTime);
    ctx.fillStyle = 'rgba(59, 130, 246, 0.15)';
    ctx.strokeStyle = '#3b82f6';
    ctx.setLineDash([2, 2]);
    ctx.fillRect(winXStart, axisY - 80, winXEnd - winXStart, 100);
    ctx.strokeRect(winXStart, axisY - 80, winXEnd - winXStart, 100);
    ctx.setLineDash([]);

    // Draw Window Label
    ctx.fillStyle = '#3b82f6';
    ctx.font = 'bold 10px font-mono';
    ctx.textAlign = 'center';
    ctx.fillText('SLIDING WINDOW', (winXStart + winXEnd) / 2, axisY - 90);

    // Draw Events as dots/markers
    pastEvents.filter(e => e.timestampMs >= viewStart).forEach(e => {
        const ex = timeToX(e.timestampMs);
        const isAllowed = e.type === 'REQUEST_ALLOWED';
        const inWindow = e.timestampMs >= windowStart;

        if (isAllowed) {
            ctx.fillStyle = inWindow ? '#10b981' : '#334155';
            ctx.beginPath();
            ctx.arc(ex, axisY, 6, 0, Math.PI * 2);
            ctx.fill();
            if (inWindow) {
                ctx.shadowBlur = 10;
                ctx.shadowColor = '#10b981';
                ctx.stroke();
                ctx.shadowBlur = 0;
            }
        } else if (e.type === 'REQUEST_BLOCKED') {
            ctx.fillStyle = '#ef4444';
            ctx.beginPath();
            ctx.moveTo(ex - 5, axisY - 5);
            ctx.lineTo(ex + 5, axisY + 5);
            ctx.moveTo(ex + 5, axisY - 5);
            ctx.lineTo(ex - 5, axisY + 5);
            ctx.stroke();
        }
    });

    // Vertical line for "Current Time"
    const nowX = timeToX(currentTime);
    ctx.strokeStyle = '#f8fafc';
    ctx.lineWidth = 1;
    ctx.beginPath();
    ctx.moveTo(nowX, axisY - 100);
    ctx.lineTo(nowX, axisY + 20);
    ctx.stroke();
    
    ctx.fillStyle = '#f8fafc';
    ctx.font = 'bold 10px font-mono';
    ctx.fillText('NOW', nowX, axisY - 110);

    // Counter Display
    ctx.fillStyle = count >= maxRequests ? '#ef4444' : '#f8fafc';
    ctx.font = '900 64px Inter';
    ctx.textAlign = 'center';
    ctx.fillText(`${count}`, width / 2, height / 2 - 80);
    
    ctx.fillStyle = '#64748b';
    ctx.font = 'bold 14px Inter';
    ctx.fillText(`REQUESTS IN SLIDING WINDOW (Limit: ${maxRequests})`, width / 2, height / 2 - 40);

    // Recent Event Indicators
    const recentEvents = pastEvents.filter(e => currentTime - e.timestampMs < 400);
    recentEvents.forEach(e => {
        const age = currentTime - e.timestampMs;
        const opacity = 1 - (age / 400);
        const yOffset = (age / 400) * 30;
        
        ctx.save();
        ctx.globalAlpha = opacity;
        ctx.textAlign = 'center';
        if (e.type === 'REQUEST_ALLOWED') {
            ctx.fillStyle = '#10b981';
            ctx.font = 'bold 14px Inter';
            ctx.fillText('ALLOWED ✓', timeToX(e.timestampMs), axisY - 20 - yOffset);
        } else if (e.type === 'REQUEST_BLOCKED') {
            ctx.fillStyle = '#ef4444';
            ctx.font = 'bold 14px Inter';
            ctx.fillText('BLOCKED ✕', timeToX(e.timestampMs), axisY + 30 + yOffset);
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
