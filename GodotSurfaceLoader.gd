extends Spatial

# This script fulfills the "Godot Source Code" requirement.
# It reads the "processed_surface.json" and renders it in the Godot Engine.

export(String, FILE, "*.json") var data_file_path

func _ready():
    load_surface_data()

func load_surface_data():
    var file = File.new()
    if not file.file_exists(data_file_path):
        print("Error: JSON file not found!")
        return

    file.open(data_file_path, File.READ)
    var json_result = JSON.parse(file.get_as_text())
    file.close()

    if json_result.error != OK:
        print("Error parsing JSON")
        return

    var data = json_result.result
    var pixels = data["pixels"]

    # Create a MultiMesh to handle thousands of thin-pixels efficiently
    var multimesh = MultiMesh.new()
    multimesh.transform_format = MultiMesh.TRANSFORM_3D
    multimesh.color_format = MultiMesh.COLOR_FLOAT
    multimesh.instance_count = pixels.size()
    
    # Use a small cube to represent the "shaved" pixel
    var mesh = CubeMesh.new()
    mesh.size = Vector3(0.1, 0.01, 0.1) # Thin pixel representation
    multimesh.mesh = mesh

    for i in range(pixels.size()):
        var p = pixels[i]
        var pos = Vector3(p["pos"]["x"], p["pos"]["y"], p["pos"]["z"])
        var col = Color(p["color"]["r"]/255.0, p["color"]["g"]/255.0, p["color"]["b"]/255.0)
        
        var t = Transform.IDENTITY
        t.origin = pos
        multimesh.set_instance_transform(i, t)
        multimesh.set_instance_color(i, col)

    var instance = MultiMeshInstance.new()
    instance.multimesh = multimesh
    add_child(instance)
    print("Surface Successfully Delivered to Godot: ", pixels.size(), " pixels loaded.")