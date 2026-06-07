#!/bin/bash
cd /root/frida/frida/subprojects/frida-core
git init 2>/dev/null
git config user.email "nosuke@local"
git config user.name "nosuke"
git add -A 2>/dev/null
git commit -m "nosuke base" 2>/dev/null
for p in /tmp/strongr-patches/*.patch; do
  name=$(basename "$p")
  echo "=== Applying $name ==="
  git am --reject "$p" 2>&1 || echo "CONFLICT"
done
echo "Done"
