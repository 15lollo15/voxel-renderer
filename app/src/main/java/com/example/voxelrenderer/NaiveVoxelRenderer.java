package com.example.voxelrenderer;

import android.opengl.Matrix;

import static android.opengl.GLES20.GL_ARRAY_BUFFER;
import static android.opengl.GLES20.GL_BACK;
import static android.opengl.GLES20.GL_CCW;
import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_CULL_FACE;
import static android.opengl.GLES20.GL_DEPTH_BUFFER_BIT;
import static android.opengl.GLES20.GL_DEPTH_TEST;
import static android.opengl.GLES20.GL_ELEMENT_ARRAY_BUFFER;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_LEQUAL;
import static android.opengl.GLES20.GL_STATIC_DRAW;
import static android.opengl.GLES20.GL_TRIANGLES;
import static android.opengl.GLES20.GL_UNSIGNED_INT;
import static android.opengl.GLES20.glBindBuffer;
import static android.opengl.GLES20.glBufferData;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glCullFace;
import static android.opengl.GLES20.glDepthFunc;
import static android.opengl.GLES20.glDrawElements;
import static android.opengl.GLES20.glEnable;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glFrontFace;
import static android.opengl.GLES20.glGenBuffers;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glVertexAttribPointer;
import static android.opengl.GLES30.glBindVertexArray;
import static android.opengl.GLES30.glDrawElementsInstanced;
import static android.opengl.GLES30.glGenVertexArrays;
import static android.opengl.GLES30.glVertexAttribDivisor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class NaiveVoxelRenderer extends BasicRenderer {
    private PlyObject cubePlyObject;
    private VlyObject modelVlyObject;

    private int shaderHandle;
    private int[] VAO;
    private int countFacesToElement;



    private float[] viewM;
    private float[] modelM;
    private float[] projM;
    private float[] MVP;
    private float[] temp;


    private int MVPloc;

    private int drawMode;


    public NaiveVoxelRenderer() {
        super();
        drawMode = GL_TRIANGLES;

        viewM = new float[16];
        modelM = new float[16];
        projM = new float[16];
        MVP = new float[16];
        temp = new float[16];
        Matrix.setIdentityM(viewM, 0);
        Matrix.setIdentityM(modelM, 0);
        Matrix.setIdentityM(projM, 0);
        Matrix.setIdentityM(MVP, 0);


    }


    public void onSurfaceChanged(GL10 gl10, int w, int h) {
        super.onSurfaceChanged(gl10, w, h);
        float aspect = ((float) w) / ((float) (h == 0 ? 1 : h));

        Matrix.perspectiveM(projM, 0, 45f, aspect, 0.1f, 100f);

        Matrix.setLookAtM(viewM, 0, 0, 0f, 60f,
                0, 0, 0,
                0, 1, 0);

    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        super.onSurfaceCreated(gl10, eglConfig);

        loadShaders();
        loadCube();
        loadVoxelModel();



        float[] vertices = cubePlyObject.getVertices();
        int[] indices = cubePlyObject.getIndices();

        float[] offsets = generateOffsets();


        FloatBuffer vertexData = allocateFloatBuffer(vertices);
        IntBuffer indexData = allocateIntBuffer(indices);
        FloatBuffer offsetsData = allocateFloatBuffer(offsets);

        countFacesToElement = indices.length;


        VAO = new int[1];
        glGenVertexArrays(1, VAO, 0);

        int[] VBO = new int[3];
        glGenBuffers(3, VBO, 0);

        glBindVertexArray(VAO[0]);
            glBindBuffer(GL_ARRAY_BUFFER, VBO[0]);
                glBufferData(GL_ARRAY_BUFFER, Float.BYTES * vertexData.capacity(), vertexData, GL_STATIC_DRAW);
                glVertexAttribPointer(1, 3, GL_FLOAT, false, Float.BYTES * 6, 0);
                glEnableVertexAttribArray(1);
            glBindBuffer(GL_ARRAY_BUFFER, 0);

            glBindBuffer(GL_ARRAY_BUFFER, VBO[2]);
                glBufferData(GL_ARRAY_BUFFER, Float.BYTES * offsetsData.capacity(), offsetsData, GL_STATIC_DRAW);
                glVertexAttribPointer(2, 3, GL_FLOAT, false, Float.BYTES * 3, 0);
                glEnableVertexAttribArray(2);
                glVertexAttribDivisor(2, 1);
            glBindBuffer(GL_ARRAY_BUFFER, 0);

            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, VBO[1]);
                glBufferData(GL_ELEMENT_ARRAY_BUFFER, Integer.BYTES * indexData.capacity(), indexData, GL_STATIC_DRAW);


        glBindVertexArray(0);

        MVPloc = glGetUniformLocation(shaderHandle, "MVP");

        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);

        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        glFrontFace(GL_CCW);

    }

    private float[] generateOffsets() {
        float[] offsets = new float[3 * modelVlyObject.getVoxelNum()];
        int[][] positions = modelVlyObject.getVoxelsPositionColorIndex();

        float marginX = (modelVlyObject.getX() - 1) * 0.5f;
        float marginY = (modelVlyObject.getY() - 1) * 0.5f;
        float marginZ = (modelVlyObject.getZ() - 1) * 0.5f;

        for (int i = 0; i < modelVlyObject.getVoxelNum(); i++) {
            int[] position = positions[i];
            offsets[i * 3] = position[0] - marginX;
            offsets[i * 3 + 1] = position[2] - marginY;
            offsets[i * 3 + 2] = position[1] - marginZ;
        }

        return offsets;
    }

    private void loadCube() {
        try {
            InputStream is = context.getAssets().open("models/pcube.ply");
            cubePlyObject = new PlyObject(is);
            cubePlyObject.parse();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void loadVoxelModel() {
        try {
            InputStream is = context.getAssets().open("models/chrk.vly");
            modelVlyObject = new VlyObject(is);
            modelVlyObject.parse();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void loadShaders() {
        try {
            InputStream isV = context.getAssets().open("shaders/vertex_shader.glslv");
            InputStream isF = context.getAssets().open("shaders/fragment_shader.glslf");
            shaderHandle = ShaderCompiler.createProgram(isV,isF);
        }catch(IOException e){
            System.exit(-1);
        }
    }

    private IntBuffer allocateIntBuffer(int[] array) {
        IntBuffer buffer =
                ByteBuffer.allocateDirect(Integer.BYTES * array.length)
                        .order(ByteOrder.nativeOrder())
                        .asIntBuffer();

        buffer.put(array);
        buffer.position(0);

        return buffer;
    }

    private FloatBuffer allocateFloatBuffer(float[] array) {
        FloatBuffer buffer =
                ByteBuffer.allocateDirect(Float.BYTES * array.length)
                        .order(ByteOrder.nativeOrder())
                        .asFloatBuffer();

        buffer.put(array);
        buffer.position(0);

        return buffer;
    }


    @Override
    public void onDrawFrame(GL10 gl10) {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        Matrix.multiplyMM(temp, 0, projM, 0, viewM, 0);

        Matrix.setIdentityM(modelM,0);
        Matrix.translateM(modelM,0,0,0,0);
        Matrix.rotateM(modelM,0,0,1,0,0);
        Matrix.multiplyMM(MVP, 0, temp, 0, modelM, 0);


        glUseProgram(shaderHandle);
            glBindVertexArray(VAO[0]);
                glUniformMatrix4fv(MVPloc, 1, false, MVP, 0);
                glDrawElementsInstanced(drawMode, countFacesToElement, GL_UNSIGNED_INT, 0, modelVlyObject.getVoxelNum());
                //glDrawElements(drawMode, countFacesToElement, GL_UNSIGNED_INT, 0);
                //glDrawArrays(drawMode, 0, 3);
            glBindVertexArray(0);
        glUseProgram(0);

    }

}
