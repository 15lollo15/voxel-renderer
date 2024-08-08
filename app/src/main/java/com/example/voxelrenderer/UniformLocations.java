package com.example.voxelrenderer;

import static android.opengl.GLES20.glGetUniformLocation;

public class UniformLocations {
    private final int MVPLocation;
    private final int modelMatrixLocation;
    private final int inverseModelMatrixLocation;
    private final int lightPositionLocation;
    private final int eyePositionLocation;
    private final int texLocation;

    private static UniformLocations uniformLocations;

    private UniformLocations(int shaderHandle) {
        MVPLocation = glGetUniformLocation(shaderHandle, "MVP");
        modelMatrixLocation = glGetUniformLocation(shaderHandle, "modelM");
        inverseModelMatrixLocation = glGetUniformLocation(shaderHandle, "inverseModelM");
        texLocation = glGetUniformLocation(shaderHandle, "tex");
        eyePositionLocation = glGetUniformLocation(shaderHandle, "eyePos");
        lightPositionLocation = glGetUniformLocation(shaderHandle, "lightPos");
    }

    public static UniformLocations getInstance(int shaderHandle) {
        if (uniformLocations == null)
            uniformLocations = new UniformLocations(shaderHandle);
        return uniformLocations;
    }

    public int getMVPLocation() {
        return MVPLocation;
    }

    public int getModelMatrixLocation() {
        return modelMatrixLocation;
    }

    public int getInverseModelMatrixLocation() {
        return inverseModelMatrixLocation;
    }

    public int getLightPositionLocation() {
        return lightPositionLocation;
    }

    public int getEyePositionLocation() {
        return eyePositionLocation;
    }

    public int getTexLocation() {
        return texLocation;
    }
}
