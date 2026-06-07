
import os

path = r'C:\Users\PC\Desktop\front-master\front\src\app\pages\staff\staff.component.ts'
with open(path, 'rb') as f:
    content = f.read()

for i, byte in enumerate(content):
    if byte == ord('`'):
        # Find line number
        line_no = content[:i].count(b'\n') + 1
        print(f"Backtick at byte {i}, line {line_no}")
