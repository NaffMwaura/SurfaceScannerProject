import json
import time
import os

class ThinPixelScanner:
    def __init__(self, thickness=0.0001):
        self.thickness = thickness
        self.scanned_pixels = []
        self.player_metadata = {}
        print(f"[SYSTEM] Shaving Engine Ready. Depth: {self.thickness}")

    def set_player_state(self, state):
        """Stores motion, inventory, and held item data."""
        self.player_metadata = state

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
                    "timestamp": time.time(),
                    "player_state": self.player_metadata
                },
                "pixels": self.scanned_pixels
            }
            with open(filename, 'w') as f:
                json.dump(output, f, indent=4)
            print(f"[SUCCESS] Shaved {len(self.scanned_pixels)} blocks into Thin-Pixels.")
            if self.player_metadata:
                motion = f"({self.player_metadata.get('motion_x', 0):.2f}, {self.player_metadata.get('motion_y', 0):.2f})"
                print(f"[MOTION] Captured Velocity: {motion}")
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
                    time.sleep(0.3) 
                    
                    with open(input_file, 'r') as f:
                        raw_data = json.load(f)
                    
                    # Handle the new dictionary structure from the latest Java code
                    # If it's a dict, get 'terrain', otherwise assume it's the old list format
                    blocks = raw_data.get("terrain", []) if isinstance(raw_data, dict) else raw_data
                    player_state = raw_data.get("player_state", {}) if isinstance(raw_data, dict) else {}

                    if not blocks:
                        print("[WARN] Scan contained no terrain data.")
                        last_mtime = current_mtime
                        continue

                    engine = ThinPixelScanner()
                    engine.set_player_state(player_state)
                    
                    print(f"[PROCESSING] Shaving {len(blocks)} blocks...")
                    for block in blocks:
                        b_type = block.get('type', 'AIR')
                        
                        # Enhanced Color Mapping
                        if "GRASS" in b_type:
                            color = (34, 139, 34)
                        elif "STONE" in b_type:
                            color = (128, 128, 128)
                        elif "DIRT" in b_type:
                            color = (139, 69, 19)
                        elif "WOOD" in b_type or "LOG" in b_type:
                            color = (101, 67, 33)
                        elif "WATER" in b_type:
                            color = (0, 0, 255)
                        else:
                            color = (200, 200, 200) # Default grey
                        
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
            pass # Skip if file is locked
        except Exception as e:
            print(f"[DEBUG] Error: {e}")
        
        time.sleep(0.5)

if __name__ == "__main__":
    try:
        monitor_minecraft_data()
    except KeyboardInterrupt:
        print("\n[SYSTEM] Scanner Offline.")