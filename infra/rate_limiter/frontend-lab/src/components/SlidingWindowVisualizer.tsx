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
  const windowSizeMs = parseInt(config.windowSizeMs || '1000');

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
    // Show a range of [currentTime - 2000, currentTime + 500]
    const viewStart = currentTime - 2000;
    const viewEnd = currentTime + 500;
    const timeToX = (t: number) => {
        const ratio = (t - viewStart) / (viewEnd - viewStart);
        return axisPadding + ratio * axisWidth;
    };

    // Draw Sliding Window "Cloth"
    const winXStart = timeToX(windowStart);
    const winXEnd = timeToX(currentTime);
    const winWidth = winXEnd - winXStart;
    
    // Cloth Gradient
    const gradient = ctx.createLinearGradient(winXStart, 0, winXEnd, 0);
    gradient.addColorStop(0, 'rgba(59, 130, 246, 0.1)');
    gradient.addColorStop(0.5, 'rgba(59, 130, 246, 0.2)');
    gradient.addColorStop(1, 'rgba(59, 130, 246, 0.1)');

    ctx.fillStyle = gradient;
    ctx.beginPath();
    ctx.roundRect(winXStart, axisY - 80, winWidth, 100, 12);
    ctx.fill();
    
    ctx.strokeStyle = '#3b82f6';
    ctx.setLineDash([5, 5]);
    ctx.lineWidth = 2;
    ctx.stroke();
    ctx.setLineDash([]);

    // Wind Effect (Moving lines inside window)
    const windOffset = (currentTime / 10) % 50;
    ctx.strokeStyle = 'rgba(255, 255, 255, 0.1)';
    ctx.lineWidth = 1;
    ctx.beginPath();
    for(let i = 0; i < 5; i++) {
        const yLine = axisY - 70 + i * 20;
        const xLine = winXStart + (windOffset + i * 30) % winWidth;
        ctx.moveTo(xLine, yLine);
        ctx.lineTo(Math.min(xLine + 20, winXEnd), yLine);
    }
    ctx.stroke();

    // Draw Window Label
    ctx.fillStyle = '#3b82f6';
    ctx.font = 'bold 10px font-mono';
    ctx.textAlign = 'center';
    ctx.fillText('SLIDING WINDOW', (winXStart + winXEnd) / 2, axisY - 90);

    // Draw Events
    pastEvents.filter(e => e.timestampMs >= viewStart).forEach(e => {
        const ex = timeToX(e.timestampMs);
        const inWindow = e.timestampMs >= windowStart;
        
        // Age for animation
        const age = currentTime - e.timestampMs;

        if (e.type === 'REQUEST_ALLOWED') {
            const isFresh = age < 200;
            ctx.fillStyle = inWindow ? '#10b981' : '#334155';
            
            // Pulse if fresh
            const radius = isFresh ? 6 + Math.sin(age * 0.1) * 2 : 6;
            
            ctx.beginPath();
            ctx.arc(ex, axisY, radius, 0, Math.PI * 2);
            ctx.fill();
            
            if (inWindow) {
                ctx.shadowBlur = 10;
                ctx.shadowColor = '#10b981';
                ctx.stroke();
                ctx.shadowBlur = 0;
            }
        } else if (e.type === 'REQUEST_BLOCKED') {
            // Blown off animation
            // Fly up and fade out
            const flyHeight = Math.min(100, age * 0.2); // Fly up to 100px
            const flyX = age * 0.1; // Drift right
            const opacity = Math.max(0, 1 - age / 600);
            
            if (opacity > 0) {
                ctx.save();
                ctx.globalAlpha = opacity;
                ctx.fillStyle = '#ef4444';
                
                // Draw as a small particle/shard
                const particleX = ex + flyX;
                const particleY = axisY - flyHeight;
                
                ctx.translate(particleX, particleY);
                ctx.rotate(age * 0.01); // Spin
                
                ctx.beginPath();
                ctx.moveTo(-4, -4);
                ctx.lineTo(4, 4);
                ctx.moveTo(4, -4);
                ctx.lineTo(-4, 4);
                ctx.strokeStyle = '#ef4444';
                ctx.lineWidth = 2;
                ctx.stroke();
                
                ctx.restore();
            }
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
    ctx.fillText(`REQUESTS IN WINDOW (Limit: ${maxRequests})`, width / 2, height / 2 - 40);

    // Recent Event Text Indicators (Static at bottom)
    const recentEvents = pastEvents.filter(e => currentTime - e.timestampMs < 300);
    recentEvents.forEach(e => {
        const age = currentTime - e.timestampMs;
        const opacity = 1 - (age / 300);
        
        ctx.save();
        ctx.globalAlpha = opacity;
        ctx.textAlign = 'center';
        if (e.type === 'REQUEST_ALLOWED') {
            ctx.fillStyle = '#10b981';
            ctx.font = 'bold 14px Inter';
            ctx.fillText('ALLOWED ✓', timeToX(e.timestampMs), axisY + 30);
        } else if (e.type === 'REQUEST_BLOCKED') {
            ctx.fillStyle = '#ef4444';
            ctx.font = 'bold 14px Inter';
            ctx.fillText('BLOCKED ✕', timeToX(e.timestampMs), axisY + 30);
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
