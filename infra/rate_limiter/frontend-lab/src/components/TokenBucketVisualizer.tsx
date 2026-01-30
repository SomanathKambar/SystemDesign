import { useEffect, useRef } from 'react';
import type { RateLimitEvent } from '../types';

interface Props {
  currentTime: number;
  events: RateLimitEvent[];
  config: Record<string, string>;
}

export const TokenBucketVisualizer = ({ currentTime, events, config }: Props) => {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const capacity = parseFloat(config.capacity || '10');

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    // Filter events up to current time
    const pastEvents = events.filter(e => e.timestampMs <= currentTime);
    
    // Find the latest state-carrying event
    let currentTokens = capacity; 
    let lastStateTime = 0;
    
    const lastEvent = [...pastEvents].reverse().find(e => 
      e.type === 'TOKEN_REFILLED' || e.type === 'REQUEST_ALLOWED' || e.type === 'REQUEST_BLOCKED'
    );

    if (lastEvent) {
      lastStateTime = lastEvent.timestampMs;
      if (lastEvent.type === 'TOKEN_REFILLED') {
        currentTokens = lastEvent.currentTokens ?? parseFloat(lastEvent.payload.tokensAfterRefill || '0');
      } else if (lastEvent.type === 'REQUEST_ALLOWED') {
        currentTokens = parseFloat(lastEvent.payload.tokensAfterConsuming || '0');
      } else {
        currentTokens = parseFloat(lastEvent.payload.tokensAfterRefill || lastEvent.payload.tokensBefore || '0');
      }
    }

    // Interpolate refill since last event
    const refillRate = parseFloat(config.rate || '1');
    const timePassed = currentTime - lastStateTime;
    const refilled = (timePassed * refillRate) / 1000;
    currentTokens = Math.min(capacity, currentTokens + refilled);

    const { width, height } = canvas;
    ctx.clearRect(0, 0, width, height);

    // Background Heartbeat Pulse
    const scanLinePos = (currentTime % 2000) / 2000;
    ctx.strokeStyle = 'rgba(59, 130, 246, 0.05)';
    ctx.lineWidth = 1;
    ctx.beginPath();
    ctx.moveTo(0, height * scanLinePos);
    ctx.lineTo(width, height * scanLinePos);
    ctx.stroke();

    // Draw Bucket
    const bucketWidth = 200;
    const bucketHeight = 300;
    const x = (width - bucketWidth) / 2;
    const y = (height - bucketHeight) / 2;

    // Bucket glass effect
    ctx.strokeStyle = '#64748b';
    ctx.lineWidth = 4;
    ctx.beginPath();
    ctx.moveTo(x, y);
    ctx.lineTo(x, y + bucketHeight);
    ctx.lineTo(x + bucketWidth, y + bucketHeight);
    ctx.lineTo(x + bucketWidth, y);
    ctx.stroke();

    // Draw Water/Tokens
    const fillHeight = (currentTokens / capacity) * bucketHeight;
    const gradient = ctx.createLinearGradient(x, y + bucketHeight - fillHeight, x, y + bucketHeight);
    gradient.addColorStop(0, '#3b82f6');
    gradient.addColorStop(1, '#1d4ed8');

    ctx.fillStyle = gradient;
    ctx.fillRect(x + 4, y + bucketHeight - fillHeight, bucketWidth - 8, fillHeight);

    // Activity Glow (Always on)
    const pulse = (Math.sin(currentTime / 200) + 1) / 2;
    ctx.shadowBlur = 5 + pulse * 10;
    ctx.shadowColor = '#3b82f6';
    ctx.strokeStyle = `rgba(59, 130, 246, ${0.3 + pulse * 0.4})`;
    ctx.lineWidth = 1;
    ctx.strokeRect(x + 2, y + bucketHeight - fillHeight - 2, bucketWidth - 4, fillHeight + 4);
    ctx.shadowBlur = 0;

    // Text info
    ctx.fillStyle = '#f8fafc';
    ctx.font = '900 64px Inter';
    ctx.textAlign = 'center';
    ctx.fillText(`${currentTokens.toFixed(1)}`, width / 2, y + bucketHeight / 2);
    
    ctx.fillStyle = 'rgba(248, 250, 252, 0.5)';
    ctx.font = 'bold 16px Inter';
    ctx.fillText(`TOKENS AVAILABLE (Limit: ${capacity})`, width / 2, y + bucketHeight + 40);

    // Recent Event Indicators
    const recentEvents = pastEvents.filter(e => currentTime - e.timestampMs < 600);
    recentEvents.forEach(e => {
        const age = currentTime - e.timestampMs;
        const opacity = 1 - (age / 600);
        const yOffset = (age / 600) * 60;

        ctx.save();
        ctx.globalAlpha = opacity;
        ctx.textAlign = 'center';
        if (e.type === 'REQUEST_ALLOWED') {
            // High impact flash on entry
            if (age < 100) {
                ctx.fillStyle = 'rgba(16, 185, 129, 0.4)';
                ctx.fillRect(x, y, bucketWidth, bucketHeight);
            }

            ctx.fillStyle = '#10b981';
            ctx.font = 'bold 18px Inter';
            ctx.fillText('ALLOWED ✓', width / 2, y - 40 - yOffset);
            
            // Draw falling token (larger and more visible)
            const dropY = y + bucketHeight + (age / 600) * 180;
            ctx.fillStyle = '#fbbf24';
            ctx.beginPath();
            ctx.arc(width / 2, dropY, 12, 0, Math.PI * 2);
            ctx.fill();
            ctx.shadowBlur = 20;
            ctx.shadowColor = '#fbbf24';
            ctx.stroke();
        } else if (e.type === 'REQUEST_BLOCKED') {
            ctx.fillStyle = '#ef4444';
            ctx.font = 'bold 20px Inter';
            ctx.fillText('BLOCKED (EMPTY) ✕', width / 2, y + bucketHeight + 80 + yOffset);
            
            // Shake the bucket
            const shake = Math.sin(age / 20) * 5;
            ctx.strokeStyle = '#ef4444';
            ctx.lineWidth = 3;
            ctx.strokeRect(x - 5 + shake, y - 5, bucketWidth + 10, bucketHeight + 10);
        }
        ctx.restore();
    });

  }, [currentTime, events, capacity]);

  return (
    <canvas 
      ref={canvasRef} 
      width={600} 
      height={400} 
      className="w-full h-full object-contain"
    />
  );
};
