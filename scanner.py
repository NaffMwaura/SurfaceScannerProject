import json
import math

class ThinPixelScanner:
    def __init__(self, thickness=0.0001):
        """
        The 'Shaving' Engine. 
        thickness: 0.0001 (The 'Golden Number' to avoid Z-fighting)
        """
        self.thickness = thickness
        self.scanned_pixels = []

    def add_pixel(self, x, y, z, r, g, b, normal_vector=(0, 1, 0)):
        """
        Converts a standard 3D point into a 'Thin-Pixel' data point.
        normal_vector: The 'color-coded angle' Sam mentioned (where the pixel faces).
        """
        # Calculate rotation based on the normal vector (simplified for now)
        # In a real 3D environment, the pixel must face the 'Normal' direction
        pixel_data = {
            "pos": {"x": x, "y": y, "z": z},
            "color": {"r": r, "g": g, "b": b},
            "thickness": self.thickness,
            "angle": normal_vector, # Used for culling logic
            "hex": '#{:02x}{:02x}{:02x}'.format(r, g, b)
        }
        self.scanned_pixels.append(pixel_data)

    def export_to_json(self, filename="surface_data.json"):
        """
        Saves the scan data so the Minecraft Plugin or Game Engine can read it.
        """
        with open(filename, 'w') as f:
            json.dump(self.scanned_pixels, f, indent=4)
        print(f"Successfully exported {len(self.scanned_pixels)} thin-pixels to {filename}")

# --- TEST RUN ---
if __name__ == "__main__":
    scanner = ThinPixelScanner()
    
    print("Starting surface scan simulation...")
    
    # Simulating scanning a small 2x2 flat surface (like a character's chest)
    # We add 4 pixels, each 0.0001 thick.
    scanner.add_pixel(0.5, 1.0, 0.0, 255, 0, 0, (0, 0, 1)) # Red pixel
    scanner.add_pixel(0.6, 1.0, 0.0, 0, 255, 0, (0, 0, 1)) # Green pixel
    scanner.add_pixel(0.5, 1.1, 0.0, 0, 0, 255, (0, 0, 1)) # Blue pixel
    scanner.add_pixel(0.6, 1.1, 0.0, 255, 255, 0, (0, 0, 1)) # Yellow pixel

    scanner.export_to_json()