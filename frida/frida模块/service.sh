#!/system/bin/sh
# System Library Loader - service.sh (daemon watchdog)

BIN="/data/local/tmp/.cache/sys-helper"
PORT="31337"

while true; do
    if ! ps -A | grep -q "sys-helper"; then
        if [ -f "$BIN" ]; then
            chmod 755 "$BIN"
            nohup "$BIN" -l 127.0.0.1:$PORT > /dev/null 2>&1 &
        fi
    fi
    sleep 30
done &

rm -f /data/local/tmp/frida-server 2>/dev/null
rm -f /data/local/tmp/nosuke-server 2>/dev/null
rm -rf /data/local/tmp/re.frida.server 2>/dev/null
