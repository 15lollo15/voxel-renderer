package com.example.voxelrenderer;

public enum ShaderLocations {
    VPOS_LOCATION(1),
    NORMALS_LOCATION(4),
    OFFSETS_LOCATION(2),
    TEXTURE_INDICES(3);

    private final int value;

    ShaderLocations(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}