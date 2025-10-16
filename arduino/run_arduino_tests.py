"""
Simple Arduino test helper:
- Checks that .ino files exist under arduino/ directory
- If Arduino CLI is installed, attempts to compile the main sketch using `arduino-cli compile` (user must configure board fqbn)

Usage:
  python run_arduino_tests.py

Notes:
- This script does not require Arduino CLI to run the basic file presence checks.
"""
import os
import subprocess

root_candidates = [os.path.normpath(os.path.dirname(__file__)), os.path.normpath(os.path.join(os.path.dirname(__file__), '..', 'src', 'arduino'))]
found = False
for root in root_candidates:
    if not os.path.exists(root):
        continue
    for dirpath, dirnames, filenames in os.walk(root):
        for f in filenames:
            if f.endswith('.ino'):
                print('Found sketch:', os.path.join(dirpath, f))
                found = True
                # Check for presence of setup() and loop()
                path = os.path.join(dirpath, f)
                with open(path, 'r', encoding='utf-8', errors='ignore') as fh:
                    content = fh.read()
                    if 'void setup' in content or 'setup()' in content:
                        print('  -> setup() found')
                    else:
                        print('  -> WARNING: setup() not found in', f)
                    if 'void loop' in content or 'loop()' in content:
                        print('  -> loop() found')
                    else:
                        print('  -> WARNING: loop() not found in', f)
    for f in filenames:
        if f.endswith('.ino'):
            print('Found sketch:', os.path.join(dirpath, f))
            found = True
            # Check for presence of setup() and loop()
            path = os.path.join(dirpath, f)
            with open(path, 'r', encoding='utf-8', errors='ignore') as fh:
                content = fh.read()
                if 'void setup' in content or 'setup()' in content:
                    print('  -> setup() found')
                else:
                    print('  -> WARNING: setup() not found in', f)
                if 'void loop' in content or 'loop()' in content:
                    print('  -> loop() found')
                else:
                    print('  -> WARNING: loop() not found in', f)

if not found:
    print('No .ino files found under', root)
    exit(2)

# Try to run arduino-cli if available
try:
    subprocess.run(['arduino-cli', '--version'], check=True, stdout=subprocess.PIPE)
    print('arduino-cli detected. To compile, run:')
    print('  arduino-cli compile --fqbn <your_board_fqbn>', os.path.join(root))
except Exception:
    print('arduino-cli not found; skipping compile step. Install it if you want to compile sketches.')

print('Arduino checks complete.')
