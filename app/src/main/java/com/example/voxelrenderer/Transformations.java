package com.example.voxelrenderer;

import android.opengl.Matrix;

public class Transformations {
    private final float[] viewMatrix;
    private final float[] modelMatrix;
    private final float[] projectionMatrix;

    public Transformations() {
        viewMatrix = new float[16];
        modelMatrix = new float[16];
        projectionMatrix = new float[16];
        Matrix.setIdentityM(viewMatrix, 0);
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.setIdentityM(projectionMatrix, 0);
    }

    public void setProjectionMatrix(float fovY, float aspect, float zNear, float zFar) {
        Matrix.perspectiveM(projectionMatrix, 0, fovY, aspect, zNear, zFar);
    }

    public void setViewMatrix(float[] eyePos) {
        Matrix.setLookAtM(viewMatrix, 0, eyePos[0], eyePos[1], eyePos[2],
                0, 0, 0,
                0, 1, 0);
    }

    public void setModelMatrix(float dx, float dy, float dz, float angleY) {
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, dx, dy, dz);
        Matrix.rotateM(modelMatrix, 0, angleY, 0, 1, 0);
    }

    public float[] getMVP() {
        float[] MVP = new float[16];
        float[] tmp = new float[16];

        Matrix.multiplyMM(tmp, 0, projectionMatrix, 0, viewMatrix, 0);
        Matrix.multiplyMM(MVP, 0, tmp, 0, modelMatrix, 0);

        return MVP;
    }

    public float[] getInvertedModelMatrix() {
        float[] invertedModelMatrix = new float[16];
        Matrix.invertM(invertedModelMatrix, 0, modelMatrix, 0);
        return invertedModelMatrix;
    }

    public float[] getModelMatrix() {
        return modelMatrix;
    }
}
