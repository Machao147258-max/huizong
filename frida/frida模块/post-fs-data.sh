#!/system/bin/sh
# System Library Loader - post-fs-data.sh
# Hidden frida-server deployment

BIN_NAME="sys-helper"
INSTALL_DIR="/data/local/tmp/.cache"
PORT="31337"

BIN_MODULE="/data/adb/modules/frida_hidden_v3/nosuke-server"
BIN_TARGET="$INSTALL_DIR/$BIN_NAME"
ANTI_DETECT="/data/adb/modules/frida_hidden_v3/anti_detect.js"

mkdir -p "$INSTALL_DIR"
chmod 700 "$INSTALL_DIR"

if [ -f "$BIN_MODULE" ]; then
    cp "$BIN_MODULE" "$BIN_TARGET"
    chmod 755 "$BIN_TARGET"
    chown root:root "$BIN_TARGET"
fi

if [ -f "$ANTI_DETECT" ]; then
    cp "$ANTI_DETECT" "$INSTALL_DIR/anti_detect.js"
    chmod 644 "$INSTALL_DIR/anti_detect.js"
fi

# Clean up any old frida-server instances
rm -f /data/local/tmp/frida-server /data/local/tmp/frida-server-* 2>/dev/null
rm -f /data/local/tmp/nosuke-server 2>/dev/null
rm -rf /data/local/tmp/re.frida.server 2>/dev/null

# Start the server
nohup "$BIN_TARGET" -l 127.0.0.1:$PORT > /dev/null 2>&1 &
