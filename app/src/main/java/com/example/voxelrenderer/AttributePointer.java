package com.example.voxelrenderer;


public class AttributePointer {
    private ShaderLocations location;
    private int size;
    private int stride;
    private int offset;
    private boolean instanced;


    public AttributePointer(ShaderLocations location, int size, int stride, int offset, boolean instanced) {
        this.location = location;
        this.size = size;
        this.stride = stride;
        this.offset = offset;
        this.instanced = instanced;
    }

    public boolean isInstanced() {
        return instanced;
    }

    public void setInstanced(boolean instanced) {
        this.instanced = instanced;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public ShaderLocations getLocation() {
        return location;
    }

    public void setLocation(ShaderLocations location) {
        this.location = location;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getStride() {
        return stride;
    }

    public void setStride(int stride) {
        this.stride = stride;
    }
}
