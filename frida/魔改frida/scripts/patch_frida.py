#!/usr/bin/env python3
import os, subprocess, glob

FDIR = os.path.expanduser("~/frida/frida/subprojects")

def find_files(exts, base=FDIR):
    files = []
    for ext in exts:
        for f in glob.glob(f"{base}/**/*.{ext}", recursive=True):
            files.append(f)
    return files

def sed_inplace(files, old, new):
    count = 0
    for f in files:
        try:
            with open(f, "r", encoding="utf-8", errors="ignore") as fh:
                content = fh.read()
            if old in content:
                content = content.replace(old, new)
                with open(f, "w", encoding="utf-8") as fh:
                    fh.write(content)
                count += 1
        except Exception:
            pass
    print(f"  {old} -> {new}: {count} files")

print("=== Patching Frida anti-detection ===")

# 1. Ports
print("[1/8] Ports...")
p = f"{FDIR}/frida-core/lib/base/socket.vala"
with open(p) as f: c = f.read()
c = c.replace("DEFAULT_CONTROL_PORT = 27042", "DEFAULT_CONTROL_PORT = 31337")
c = c.replace("DEFAULT_CLUSTER_PORT = 27052", "DEFAULT_CLUSTER_PORT = 31347")
with open(p, "w") as f: f.write(c)

p2 = f"{FDIR}/frida-gum/bindings/gumjs/guminspectorserver.c"
with open(p2) as f: c = f.read()
c = c.replace("GUM_INSPECTOR_DEFAULT_PORT 9229", "GUM_INSPECTOR_DEFAULT_PORT 19527")
with open(p2, "w") as f: f.write(c)
print("  Ports done")

# 2. D-Bus / identifier prefix
print("[2/8] D-Bus identifiers...")
exts = ["vala","c","h","m","plist","xcent","java"]
sed_inplace(find_files(exts), "re.frida", "com.nosuke")
sed_inplace(find_files(exts), "re/frida", "com/nosuke")
# Also handle Makefile and meson.build
sed_inplace(find_files(["vala","c","h","m","plist","xcent","java"]), "re.frida", "com.nosuke")
exts2 = ["vala","c","h","m","plist","xcent","java","Makefile","build"]
for f in glob.glob(f"{FDIR}/**/meson.build", recursive=True):
    exts2.append(f.replace(FDIR+"/", ""))
for f in glob.glob(f"{FDIR}/**/Makefile", recursive=True):
    try:
        with open(f) as fh: c = fh.read()
        if "re.frida" in c:
            with open(f, "w") as fh: fh.write(c.replace("re.frida", "com.nosuke"))
    except: pass

# 3. Agent library names
print("[3/8] Agent library names...")
sed_inplace(find_files(["vala","c","h","rs"]), "frida-agent", "nosuke-agent")

# 4. Socket path
print("[4/8] Socket paths...")
sed_inplace(find_files(["c","h"]), "frida-zymbiote", "nosuke-zymbiote")

# 5. Thread names
print("[5/8] Thread names...")
p3 = f"{FDIR}/frida-gum/gum/backend-linux/gumprocess-linux.c"
with open(p3) as f: c = f.read()
c = c.replace("gum-modify-thread-worker", "nosuke-modify-thread-worker")
with open(p3, "w") as f: f.write(c)
print("  Thread names done")

# 6. HTTP headers
print("[6/8] HTTP headers...")
p4 = f"{FDIR}/frida-core/lib/base/socket.vala"
with open(p4) as f: c = f.read()
c = c.replace('"User-Agent", "Frida/', '"User-Agent", "Nosuke/')
c = c.replace('"Server", "Frida/', '"Server", "Nosuke/')
c = c.replace('"Frida/" + _version_string', '"Nosuke/" + _version_string')
with open(p4, "w") as f: f.write(c)
print("  HTTP headers done")

# 7. Entrypoint
print("[7/8] Entrypoint...")
sed_inplace(find_files(["vala","c","h","rs"]), "frida_agent_main", "nosuke_agent_main")

# 8. Worker names and helper
print("[8/8] Worker/helper strings...")
sed_inplace(find_files(["vala","c","h","rs","m"]), "frida_agent_worker", "nosuke_agent_worker")
sed_inplace(find_files(["vala","c","h","m","plist","java"]), "frida-helper", "nosuke-helper")

print("=== Patch complete ===")
