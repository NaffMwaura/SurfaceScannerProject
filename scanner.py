import json
import time
import os
import random

class ThinPixelScanner:
    def __init__(self, thickness=0.0001):
        self.thickness = thickness
        self.scanned_pixels = []
        print(f"[SYSTEM] Shaving Engine Ready. Depth: {self.thickness}")

    def add_pixel(self, x, y, z, r, g, b, normal_vector=(0, 0, 1)):
        pixel_data = {
            "id": len(self.scanned_pixels),
            "pos": {"x": float(x), "y": float(y), "z": float(z)},
            "color": {"r": r, "g": g, "b": b},
            "thickness": self.thickness,
            "angle": normal_vector, 
            "hex": '#{:02x}{:02x}{:02x}'.format(r, g, b)
        }
        self.scanned_pixels.append(pixel_data)

    def export_processed_data(self, filename="processed_surface.json"):
        try:
            output = {
                "metadata": {
                    "total_pixels": len(self.scanned_pixels),
                    "shave_depth": self.thickness,
                    "timestamp": time.time()
                },
                "pixels": self.scanned_pixels
            }
            with open(filename, 'w') as f:
                json.dump(output, f, indent=4)
            print(f"[SUCCESS] Shaved {len(self.scanned_pixels)} blocks into Thin-Pixels.")
        except Exception as e:
            print(f"[ERROR] Save failed: {e}")

def monitor_minecraft_data(input_file="surface_data.json"):
    last_mtime = 0
    print(f"[*] Monitoring: {input_file}")
    print("[*] Use the STICK in Minecraft to trigger...")
    
    while True:
        try:
            if os.path.exists(input_file):
                current_mtime = os.path.getmtime(input_file)
                
                if current_mtime != last_mtime:
                    # Give Java a moment to finish writing
                    time.sleep(0.2) 
                    
                    with open(input_file, 'r') as f:
                        raw_data = json.load(f)
                    
                    # FIX: Check if raw_data is a list (from Java) or dict
                    blocks = raw_data if isinstance(raw_data, list) else raw_data.get("pixels", [])
                    
                    if not blocks:
                        print("[WARN] Scan was empty.")
                        last_mtime = current_mtime
                        continue

                    engine = ThinPixelScanner()
                    
                    print(f"[PROCESSING] Shaving {len(blocks)} blocks...")
                    for block in blocks:
                        # Map Minecraft materials to colors
                        b_type = block.get('type', 'AIR')
                        color = (34, 139, 34) if "GRASS" in b_type else (128, 128, 128)
                        
                        engine.add_pixel(
                            block['x'], 
                            block['y'] + 1.0, 
                            block['z'], 
                            *color
                        )
                    
                    engine.export_processed_data()
                    last_mtime = current_mtime
                    print("[*] Ready for next scan.")
            
        except PermissionError:
            # File is being locked by Java, just skip this tick
            pass
        except Exception as e:
            print(f"[DEBUG] Error details: {type(e).__name__} - {e}")
        
        time.sleep(0.5)

if __name__ == "__main__":
    try:
        monitor_minecraft_data()
    except KeyboardInterrupt:
        print("\n[SYSTEM] Scanner Offline.")