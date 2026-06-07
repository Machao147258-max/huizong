import sys
data = open("/root/frida/frida/build/subprojects/frida-core/server/nosuke-server", "rb").read()
old = b"/frida-zymbiote-00000000000000000000000000000000"
new = b"/nosuk-zymbiote-00000000000000000000000000000000"
assert len(old) == len(new), f"{len(old)} != {len(new)}"
count = data.count(old)
data = data.replace(old, new)
open("/root/frida/frida/build/subprojects/frida-core/server/nosuke-server", "wb").write(data)
print(f"Patched {count} occurrences")
