
# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path

import json
import os
from pathlib import Path

def thickness(size, insulated):
    return {1:4,2:6,4:8,8:12,12:16}[size] if insulated else [2,3,4,6,7,7,8,8,9,10,11,12,13,14,15,16][size-1]

def fmt(v):
    if v == int(v):
        return int(v)
    return round(v, 2)

def make_core_model(size, insulated):
    t = thickness(size, insulated)
    half = t / 2.0
    mn = 8.0 - half
    mx = 8.0 + half

    if insulated:
        overlay_tex = 'gt6ce:block/material_icons/rubber/pipeside'
        overlay_tint = 1
    else:
        overlay_tex = 'gt6ce:block/material_icons/metallic/wire_overlay'
        overlay_tint = None

    textures = {
        'wire': 'gt6ce:block/material_icons/metallic/wire',
        'overlay': overlay_tex
    }

    all_faces = ['down','up','north','south','west','east']
    elements = []

    # Wire layer
    wire_faces = {}
    for face in all_faces:
        wire_faces[face] = {'texture': '#wire', 'tintindex': 0}
    elements.append({'from': [fmt(mn), fmt(mn), fmt(mn)], 'to': [fmt(mx), fmt(mx), fmt(mx)], 'faces': wire_faces})

    # Overlay layer
    overlay_faces = {}
    for face in all_faces:
        fdata = {'texture': '#overlay'}
        if overlay_tint is not None:
            fdata['tintindex'] = overlay_tint
        overlay_faces[face] = fdata
    elements.append({'from': [fmt(mn), fmt(mn), fmt(mn)], 'to': [fmt(mx), fmt(mx), fmt(mx)], 'faces': overlay_faces})

    return {'parent': 'block/block', 'textures': textures, 'elements': elements}

def make_item_model(size, insulated):
    t = thickness(size, insulated)
    half = t / 2.0
    mn = 8.0 - half
    mx = 8.0 + half

    if insulated:
        overlay_tex = 'gt6ce:block/material_icons/rubber/pipeside'
        overlay_tint = 1
    else:
        overlay_tex = 'gt6ce:block/material_icons/metallic/wire_overlay'
        overlay_tint = None

    textures = {
        'wire': 'gt6ce:block/material_icons/metallic/wire',
        'overlay': overlay_tex
    }

    all_faces = ['down','up','north','south','west','east']
    elements = []

    # Core - wire layer
    core_wire = {}
    for face in all_faces:
        core_wire[face] = {'texture': '#wire', 'tintindex': 0}
    elements.append({'from': [fmt(mn), fmt(mn), fmt(mn)], 'to': [fmt(mx), fmt(mx), fmt(mx)], 'faces': core_wire})

    # Core - overlay layer
    core_overlay = {}
    for face in all_faces:
        fdata = {'texture': '#overlay'}
        if overlay_tint is not None:
            fdata['tintindex'] = overlay_tint
        core_overlay[face] = fdata
    elements.append({'from': [fmt(mn), fmt(mn), fmt(mn)], 'to': [fmt(mx), fmt(mx), fmt(mx)], 'faces': core_overlay})

    # North arm - wire layer
    north_wire = {}
    for face in all_faces:
        north_wire[face] = {'texture': '#wire', 'tintindex': 0}
    elements.append({'from': [fmt(mn), fmt(mn), 0.0], 'to': [fmt(mx), fmt(mx), fmt(mn)], 'faces': north_wire})

    # North arm - overlay layer
    north_overlay = {}
    if insulated:
        for face in all_faces:
            if face == 'north':
                continue
            north_overlay[face] = {'texture': '#overlay', 'tintindex': overlay_tint}
    else:
        for face in all_faces:
            north_overlay[face] = {'texture': '#overlay'}
    elements.append({'from': [fmt(mn), fmt(mn), 0.0], 'to': [fmt(mx), fmt(mx), fmt(mn)], 'faces': north_overlay})

    # South arm - wire layer
    south_wire = {}
    for face in all_faces:
        south_wire[face] = {'texture': '#wire', 'tintindex': 0}
    elements.append({'from': [fmt(mn), fmt(mn), fmt(mx)], 'to': [fmt(mx), fmt(mx), 16.0], 'faces': south_wire})

    # South arm - overlay layer
    south_overlay = {}
    if insulated:
        for face in all_faces:
            if face == 'south':
                continue
            south_overlay[face] = {'texture': '#overlay', 'tintindex': overlay_tint}
    else:
        for face in all_faces:
            south_overlay[face] = {'texture': '#overlay'}
    elements.append({'from': [fmt(mn), fmt(mn), fmt(mx)], 'to': [fmt(mx), fmt(mx), 16.0], 'faces': south_overlay})

    return {'parent': 'minecraft:block/block', 'textures': textures, 'elements': elements}

def make_side_model(size, insulated):
    t = thickness(size, insulated)
    half = t / 2.0
    mn = 8.0 - half
    mx = 8.0 + half

    if insulated:
        overlay_tex = 'gt6ce:block/material_icons/rubber/pipeside'
        overlay_tint = 1
    else:
        overlay_tex = 'gt6ce:block/material_icons/metallic/wire_overlay'
        overlay_tint = None

    textures = {
        'wire': 'gt6ce:block/material_icons/metallic/wire',
        'overlay': overlay_tex
    }

    all_faces = ['down','up','north','south','west','east']
    elements = []

    # Wire layer on arm
    arm_wire = {}
    for face in all_faces:
        arm_wire[face] = {'texture': '#wire', 'tintindex': 0}
    elements.append({'from': [fmt(mn), fmt(mn), 0.0], 'to': [fmt(mx), fmt(mx), fmt(mn)], 'faces': arm_wire})

    # Overlay layer on arm
    arm_overlay = {}
    if insulated:
        for face in all_faces:
            if face == 'north':
                continue
            arm_overlay[face] = {'texture': '#overlay', 'tintindex': overlay_tint}
    else:
        for face in all_faces:
            arm_overlay[face] = {'texture': '#overlay'}
    elements.append({'from': [fmt(mn), fmt(mn), 0.0], 'to': [fmt(mx), fmt(mx), fmt(mn)], 'faces': arm_overlay})

    return {'parent': 'block/block', 'textures': textures, 'elements': elements}

base_blocks = native_path(Path(__file__).resolve().parents[1] / 'src/main/resources/assets/gt6ce/models/block/blocks')
base_machine = native_path(Path(__file__).resolve().parents[1] / 'src/main/resources/assets/gt6ce/models/block/machine')

count = 0

# Wire sizes 1-16
for size in range(1, 17):
    # Item model
    path = os.path.join(base_blocks, f'wire_item_{size:02d}.json')
    model = make_item_model(size, False)
    with open(path, 'w') as f:
        json.dump(model, f, indent=2)
    count += 1

    # Core model
    path = os.path.join(base_machine, f'wire_{size:02d}_core.json')
    model = make_core_model(size, False)
    with open(path, 'w') as f:
        json.dump(model, f, indent=2)
    count += 1

    # Side model
    path = os.path.join(base_machine, f'wire_{size:02d}_side.json')
    model = make_side_model(size, False)
    with open(path, 'w') as f:
        json.dump(model, f, indent=2)
    count += 1

# Cable sizes 1,2,4,8,12,16
for size in [1, 2, 4, 8, 12]:
    # Item model
    path = os.path.join(base_blocks, f'cable_item_{size:02d}.json')
    model = make_item_model(size, True)
    with open(path, 'w') as f:
        json.dump(model, f, indent=2)
    count += 1

    # Core model
    path = os.path.join(base_machine, f'cable_{size:02d}_core.json')
    model = make_core_model(size, True)
    with open(path, 'w') as f:
        json.dump(model, f, indent=2)
    count += 1

    # Side model
    path = os.path.join(base_machine, f'cable_{size:02d}_side.json')
    model = make_side_model(size, True)
    with open(path, 'w') as f:
        json.dump(model, f, indent=2)
    count += 1

print(f'Generated {count} model files')

# Print thickness comparison
print()
print('Wire thickness:')
for size in range(1, 17):
    print(f'  {size:2d}x: {thickness(size, False):.1f}px  half={thickness(size, False)/2:.2f}  from={8-thickness(size,False)/2:.2f}  to={8+thickness(size,False)/2:.2f}')
print()
print('Cable thickness:')
for size in [1, 2, 4, 8, 12]:
    print(f'  {size:2d}x: {thickness(size, True):.1f}px  half={thickness(size, True)/2:.2f}  from={8-thickness(size,True)/2:.2f}  to={8+thickness(size,True)/2:.2f}')
