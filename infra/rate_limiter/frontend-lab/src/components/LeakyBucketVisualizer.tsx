import { useEffect, useRef } from 'react';
import type { RateLimitEvent } from '../types';

interface Props {
  currentTime: number;
  events: RateLimitEvent[];
  config: Record<string, string>;
}

export const LeakyBucketVisualizer = ({ currentTime, events, config }: Props) => {
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
    let waterLevel = 0; 
    let lastStateTime = 0;
    
    const lastEvent = [...pastEvents].reverse().find(e => 
      e.type === 'REQUEST_ALLOWED' || e.type === 'REQUEST_BLOCKED' || e.type === 'LEAK_OCCURRED' || e.type === 'TICK'
    );

    if (lastEvent) {
      lastStateTime = lastEvent.timestampMs;
      if (lastEvent.type === 'LEAK_OCCURRED') {
        waterLevel = lastEvent.waterLevelAfterLeak ?? parseFloat(lastEvent.payload.waterLevelAfterLeak || '0');
      } else {
        waterLevel = parseFloat(lastEvent.payload.newWaterLevel || lastEvent.payload.waterAfterLeak || '0');
      }
    }

    // Interpolate leak since last event
    const leakRate = parseFloat(config.rate || '1');
    const timePassed = currentTime - lastStateTime;
    const leaked = (timePassed * leakRate) / 1000;
    waterLevel = Math.max(0, waterLevel - leaked);

    // Clear and Draw
    const { width, height } = canvas;
    ctx.clearRect(0, 0, width, height);

    // Draw Funnel/Bucket
    const bucketWidth = 200;
    const bucketHeight = 300;
    const x = (width - bucketWidth) / 2;
    const y = (height - bucketHeight) / 2;

    // Bucket Outline (with a hole at the bottom)
    ctx.strokeStyle = '#64748b';
    ctx.lineWidth = 4;
    ctx.beginPath();
    ctx.moveTo(x, y);
    ctx.lineTo(x, y + bucketHeight);
    ctx.lineTo(x + bucketWidth / 2 - 10, y + bucketHeight); // Hole left
    ctx.moveTo(x + bucketWidth / 2 + 10, y + bucketHeight); // Hole right
    ctx.lineTo(x + bucketWidth, y + bucketHeight);
    ctx.lineTo(x + bucketWidth, y);
    ctx.stroke();

    // Draw Water
    const fillHeight = (waterLevel / capacity) * bucketHeight;
    if (fillHeight > 0) {
        const gradient = ctx.createLinearGradient(x, y + bucketHeight - fillHeight, x, y + bucketHeight);
        gradient.addColorStop(0, '#0ea5e9');
        gradient.addColorStop(1, '#0284c7');

        ctx.fillStyle = gradient;
        ctx.fillRect(x + 4, y + bucketHeight - fillHeight, bucketWidth - 8, fillHeight);
        
        // Drip effect from the hole if there is water
        const dripAge = (currentTime % 500) / 500;
        const dripY = y + bucketHeight + dripAge * 40;
        ctx.fillStyle = '#0ea5e9';
        ctx.beginPath();
        ctx.arc(width / 2, dripY, 3, 0, Math.PI * 2);
        ctx.fill();
    }

    // Text info
    ctx.fillStyle = '#f8fafc';
    ctx.font = '900 64px Inter';
    ctx.textAlign = 'center';
    ctx.fillText(`${waterLevel.toFixed(1)}`, width / 2, y + bucketHeight / 2);
    
    ctx.fillStyle = 'rgba(248, 250, 252, 0.5)';
    ctx.font = 'bold 16px Inter';
    ctx.fillText(`WATER LEVEL (Capacity: ${capacity})`, width / 2, y + bucketHeight + 40);

    // Recent Event Indicators
    const recentEvents = pastEvents.filter(e => currentTime - e.timestampMs < 400);
    recentEvents.forEach(e => {
        const age = currentTime - e.timestampMs;
        const opacity = 1 - (age / 400);
        const yOffset = (age / 400) * 50;

        ctx.save();
        ctx.globalAlpha = opacity;
        ctx.textAlign = 'center';
        if (e.type === 'REQUEST_ALLOWED') {
            ctx.fillStyle = '#10b981';
            ctx.font = 'bold 14px Inter';
            ctx.fillText('ALLOWED (ADDED WATER) ✓', width / 2, y - 40 - yOffset);
            
            // Draw falling request
            const dropY = y + (age / 400) * bucketHeight;
            ctx.fillStyle = '#0ea5e9';
            ctx.beginPath();
            ctx.arc(width / 2, dropY, 8, 0, Math.PI * 2);
            ctx.fill();
        } else if (e.type === 'REQUEST_BLOCKED') {
            ctx.fillStyle = '#ef4444';
            ctx.font = 'bold 16px Inter';
            ctx.fillText('BLOCKED (OVERFLOW) ✕', width / 2, y + bucketHeight + 80 + yOffset);
            
            // Overflow effect
            ctx.strokeStyle = '#ef4444';
            ctx.lineWidth = 2;
            ctx.strokeRect(x - 5, y - 5, bucketWidth + 10, bucketHeight + 10);
        }
        ctx.restore();
    });

  }, [currentTime, events, capacity, config.rate]);

  return (
    <canvas 
      ref={canvasRef} 
      width={600} 
      height={400} 
      className="w-full h-full object-contain"
    />
  );
};
