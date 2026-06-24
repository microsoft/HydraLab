#!/bin/bash
# Cleanup script for stale iOS port forwarding processes
# Run this before starting test runs to prevent port conflicts

echo "🧹 Cleaning up stale pymobiledevice3 processes..."

# Kill all pymobiledevice3 usbmux forward processes
FORWARD_PIDS=$(ps aux | grep "pymobiledevice3 usbmux forward" | grep -v grep | awk '{print $2}')
if [ -n "$FORWARD_PIDS" ]; then
    echo "Found stale port forwarding processes: $FORWARD_PIDS"
    echo "$FORWARD_PIDS" | xargs kill -9 2>/dev/null
    echo "✅ Killed port forwarding processes"
else
    echo "✅ No stale port forwarding processes found"
fi

# Kill any orphaned pymobiledevice3 processes
ORPHANED_PIDS=$(ps aux | grep "pymobiledevice3" | grep -v grep | grep -v "cleanup_ios_ports" | awk '{print $2}')
if [ -n "$ORPHANED_PIDS" ]; then
    echo "Found orphaned pymobiledevice3 processes: $ORPHANED_PIDS"
    echo "$ORPHANED_PIDS" | xargs kill -9 2>/dev/null
    echo "✅ Killed orphaned processes"
else
    echo "✅ No orphaned pymobiledevice3 processes found"
fi

# Check for ports occupied by pymobiledevice3 processes
echo ""
echo "Checking for ports occupied by pymobiledevice3..."
PYMOBILE_PIDS=$(ps aux | grep "pymobiledevice3" | grep -v grep | grep -v "cleanup_ios_ports" | awk '{print $2}')
if [ -n "$PYMOBILE_PIDS" ]; then
    for pid in $PYMOBILE_PIDS; do
        PORTS=$(lsof -Pan -p $pid -i 2>/dev/null | grep LISTEN | awk '{print $9}' | cut -d: -f2)
        if [ -n "$PORTS" ]; then
            echo "⚠️  pymobiledevice3 (PID $pid) is using ports: $PORTS"
        fi
    done
fi

echo ""
echo "✨ Cleanup complete!"
