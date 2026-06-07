import os

base = os.path.expanduser("~/frida/frida/subprojects")

# 1. Fix agent_name in frida-core/meson.build
p = f"{base}/frida-core/meson.build"
with open(p) as f: c = f.read()
c = c.replace("agent_name = 'frida-agent' + shlib_suffix", "agent_name = 'nosuke-agent' + shlib_suffix")
with open(p, "w") as f: f.write(c)
print("1. agent_name changed")

# 2. Fix server_name in frida-core/server/meson.build
p = f"{base}/frida-core/server/meson.build"
with open(p) as f: c = f.read()
c = c.replace("server_name = 'frida-server' + exe_suffix", "server_name = 'nosuke-server' + exe_suffix")
c = c.replace("raw_server = executable('frida-server-raw'", "raw_server = executable('nosuke-server-raw'")
c = c.replace("custom_target('frida-server',", "custom_target('nosuke-server',")
c = c.replace("custom_target('frida-server-universal'", "custom_target('nosuke-server-universal'")
with open(p, "w") as f: f.write(c)
print("2. server_name changed")

# 3. Fix embed-agent.py
p = f"{base}/frida-core/src/embed-agent.py"
with open(p) as f: c = f.read()
c = c.replace("frida-agent-", "nosuke-agent-")
c = c.replace("frida-agent.", "nosuke-agent.")
c = c.replace("frida-data-agent", "nosuke-data-agent")
with open(p, "w") as f: f.write(c)
print("3. embed-agent changed")

# 4. Fix agent meson.build identity and symfile names
p = f"{base}/frida-core/lib/agent/meson.build"
with open(p) as f: c = f.read()
c = c.replace("identity = 'FridaAgent'", "identity = 'NosukeAgent'")
c = c.replace("vala_header: 'frida-agent.h'", "vala_header: 'nosuke-agent.h'")
c = c.replace("vs_module_defs: 'frida-agent.def'", "vs_module_defs: 'nosuke-agent.def'")
c = c.replace("'frida-agent.symbols'", "'nosuke-agent.symbols'")
c = c.replace("'frida-agent-x86.symbols'", "'nosuke-agent-x86.symbols'")
c = c.replace("'frida-agent-glibc.version'", "'nosuke-agent-glibc.version'")
c = c.replace("'frida-agent-android.version'", "'nosuke-agent-android.version'")
c = c.replace("'frida-agent.version'", "'nosuke-agent.version'")
with open(p, "w") as f: f.write(c)
print("4. agent meson.build changed")

print("=== Build system patch complete ===")
