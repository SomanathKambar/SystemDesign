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

    // Background Heartbeat Pulse
    const scanLinePos = (currentTime % 2000) / 2000;
    ctx.strokeStyle = 'rgba(34, 211, 238, 0.05)';
    ctx.lineWidth = 1;
    ctx.beginPath();
    ctx.moveTo(0, height * scanLinePos);
    ctx.lineTo(width, height * scanLinePos);
    ctx.stroke();

    // Draw Funnel/Bucket
    const bucketWidth = 200;
    const bucketHeight = 300;
    const x = (width - bucketWidth) / 2;
    const y = (height - bucketHeight) / 2;

    // Bucket Outline (with a hole at the bottom)
    ctx.strokeStyle = '#94a3b8';
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
        gradient.addColorStop(0, '#22d3ee');
        gradient.addColorStop(1, '#06b6d4');

        ctx.fillStyle = gradient;
        ctx.fillRect(x + 4, y + bucketHeight - fillHeight, bucketWidth - 8, fillHeight);
        
        // Activity Glow
        const pulse = (Math.sin(currentTime / 300) + 1) / 2;
        ctx.shadowBlur = 10 + pulse * 10;
        ctx.shadowColor = '#22d3ee';
        ctx.strokeStyle = `rgba(34, 211, 238, ${0.4 + pulse * 0.4})`;
        ctx.lineWidth = 2;
        ctx.strokeRect(x + 2, y + bucketHeight - fillHeight - 2, bucketWidth - 4, fillHeight + 4);
        ctx.shadowBlur = 0;

        // Continuous Drip effect from the hole if there is water
        // Draw multiple small droplets falling
        const numDroplets = 5;
        ctx.fillStyle = '#22d3ee';
        for (let i = 0; i < numDroplets; i++) {
            const offset = (currentTime + i * 100) % 500;
            const dropY = y + bucketHeight + offset * 0.3; // Speed
            const dropAlpha = 1 - (offset / 500);
            
            ctx.globalAlpha = dropAlpha;
            ctx.beginPath();
            ctx.arc(width / 2, dropY, 3, 0, Math.PI * 2);
            ctx.fill();
        }
        ctx.globalAlpha = 1.0;
    }

    // Text info
    ctx.fillStyle = '#f8fafc';
    ctx.font = '900 64px Inter';
    ctx.textAlign = 'center';
    ctx.fillText(`${waterLevel.toFixed(1)}`, width / 2, y + bucketHeight / 2);
    
    ctx.fillStyle = '#94a3b8';
    ctx.font = 'bold 16px Inter';
    ctx.fillText(`WATER LEVEL (Capacity: ${capacity})`, width / 2, y + bucketHeight + 40);

    // Recent Event Indicators
    const recentEvents = pastEvents.filter(e => currentTime - e.timestampMs < 800);
    recentEvents.forEach(e => {
        const age = currentTime - e.timestampMs;
        const opacity = 1 - (age / 800);
        
        ctx.save();
        ctx.globalAlpha = opacity;
        ctx.textAlign = 'center';

        if (e.type === 'REQUEST_ALLOWED') {
            // Flash on top
            if (age < 150) {
                ctx.fillStyle = 'rgba(16, 185, 129, 0.4)';
                ctx.fillRect(x, y, bucketWidth, 20); // Flash at rim
            }

            // Draw falling request (larger drop)
            const dropY = y + (age / 800) * bucketHeight; // Fall through bucket
            ctx.fillStyle = '#10b981'; // Green for allowed
            ctx.beginPath();
            ctx.arc(width / 2, dropY, 12, 0, Math.PI * 2);
            ctx.fill();
            ctx.shadowBlur = 15;
            ctx.shadowColor = '#10b981';
            ctx.stroke();

            // Text
            ctx.fillStyle = '#10b981';
            ctx.font = 'bold 18px Inter';
            ctx.fillText('ALLOWED ✓', width / 2, y - 20 - (age/10));

        } else if (e.type === 'REQUEST_BLOCKED') {
            // Bounce off animation
            const bounceHeight = 100;
            const progress = age / 800;
            // Parabola: y = -4x(x-1) is 0 at 0 and 1, peak at 0.5
            // We want it to start at rim (y) and go up and out
            const arcX = width / 2 + (Math.random() > 0.5 ? 1 : -1) * (progress * 150 + 20); // Move sideways
            const arcY = y - Math.sin(progress * Math.PI) * bounceHeight;

            ctx.fillStyle = '#ef4444';
            ctx.beginPath();
            ctx.arc(arcX, arcY, 8, 0, Math.PI * 2);
            ctx.fill();

            ctx.strokeStyle = '#ef4444';
            ctx.lineWidth = 2;
            ctx.beginPath();
            ctx.moveTo(arcX - 5, arcY - 5);
            ctx.lineTo(arcX + 5, arcY + 5);
            ctx.moveTo(arcX + 5, arcY - 5);
            ctx.lineTo(arcX - 5, arcY + 5);
            ctx.stroke();

            // Text
            ctx.fillStyle = '#ef4444';
            ctx.font = 'bold 16px Inter';
            ctx.fillText('BLOCKED ✕', arcX, arcY - 20);
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
