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
    let currentTokens = capacity; // Start full
    const lastEvent = [...pastEvents].reverse().find(e => 
      e.type === 'TOKEN_REFILLED' || e.type === 'REQUEST_ALLOWED' || e.type === 'REQUEST_BLOCKED'
    );

    if (lastEvent) {
      if (lastEvent.type === 'TOKEN_REFILLED') {
        currentTokens = parseFloat(lastEvent.payload.tokensAfterRefill || '0');
      } else if (lastEvent.type === 'REQUEST_ALLOWED') {
        currentTokens = parseFloat(lastEvent.payload.tokensAfterConsuming || '0');
      } else {
        currentTokens = parseFloat(lastEvent.payload.tokensAfterRefill || lastEvent.payload.tokensBefore || '0');
      }
    }

    // Clear and Draw
    const { width, height } = canvas;
    ctx.clearRect(0, 0, width, height);

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

    // Glow for liquid
    if (fillHeight > 0) {
        ctx.shadowBlur = 15;
        ctx.shadowColor = '#3b82f6';
        ctx.strokeRect(x + 4, y + bucketHeight - fillHeight, bucketWidth - 8, 1);
        ctx.shadowBlur = 0;
    }

    // Text info
    ctx.fillStyle = '#f8fafc';
    ctx.font = 'bold 16px Inter, system-ui';
    ctx.textAlign = 'center';
    ctx.fillText(`${currentTokens.toFixed(1)} / ${capacity}`, width / 2, y + bucketHeight + 30);

    // Draw falling tokens (if a request was allowed recently)
    const recentAllowed = pastEvents.slice(-3).find(e => e.type === 'REQUEST_ALLOWED' && currentTime - e.timestampMs < 300);
    if (recentAllowed) {
        const age = currentTime - recentAllowed.timestampMs;
        const dropY = y + bucketHeight + (age / 300) * 100;
        ctx.fillStyle = '#fbbf24';
        ctx.beginPath();
        ctx.arc(width / 2, dropY, 8, 0, Math.PI * 2);
        ctx.fill();
        ctx.shadowBlur = 10;
        ctx.shadowColor = '#fbbf24';
        ctx.stroke();
        ctx.shadowBlur = 0;
    }

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
