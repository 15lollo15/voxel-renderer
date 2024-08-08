package com.example.voxelrenderer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import static android.opengl.GLES20.GL_ARRAY_BUFFER;
import static android.opengl.GLES20.GL_BACK;
import static android.opengl.GLES20.GL_CCW;
import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_CULL_FACE;
import static android.opengl.GLES20.GL_DEPTH_BUFFER_BIT;
import static android.opengl.GLES20.GL_DEPTH_TEST;
import static android.opengl.GLES20.GL_ELEMENT_ARRAY_BUFFER;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_INT;
import static android.opengl.GLES20.GL_LEQUAL;
import static android.opengl.GLES20.GL_NEAREST;
import static android.opengl.GLES20.GL_STATIC_DRAW;
import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TEXTURE_MAG_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_MIN_FILTER;
import static android.opengl.GLES20.GL_TRIANGLES;
import static android.opengl.GLES20.GL_UNSIGNED_INT;
import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glBindBuffer;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glBufferData;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glCullFace;
import static android.opengl.GLES20.glDepthFunc;
import static android.opengl.GLES20.glEnable;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glFrontFace;
import static android.opengl.GLES20.glGenBuffers;
import static android.opengl.GLES20.glGenTextures;
import static android.opengl.GLES20.glGenerateMipmap;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glTexParameteri;
import static android.opengl.GLES20.glUniform1i;
import static android.opengl.GLES20.glUniform3fv;
import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glVertexAttribPointer;
import static android.opengl.GLES30.glBindVertexArray;
import static android.opengl.GLES30.glDrawElementsInstanced;
import static android.opengl.GLES30.glGenVertexArrays;
import static android.opengl.GLES30.glVertexAttribDivisor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class NaiveVoxelRenderer extends BasicRenderer {
    public static final int VPOS_LOCATION = 1;
    public static final int NORMALS_LOCATION = 4;
    public static final int OFFSETS_LOCATION = 2;
    public static final int TEXTURE_INDICES = 3;

    private PlyObject cubePlyObject;
    private VlyObject modelVlyObject;
    private float angle;

    private int shaderHandle;
    private int[] VAO;
    private int[] VBO;
    private int countFacesToElement;
    private int[] texObjId;

    private float[] eyePos;
    private float[] lightPos;

    private float[] viewM;
    private float[] modelM;
    private float[] inverseModelM;
    private float[] projM;
    private float[] MVP;
    private float[] temp;

    private int MVPloc;
    private int modelMloc;
    private int inverseModelMloc;
    private int lightPosloc;
    private int eyePosloc;

    private int texUnit;

    private int drawMode;

    private GestureDetector gestureDetector;
    private ScaleGestureDetector scaleDetector;

    private float maxZoom;
    private float minZoom;
    private float zoom;

    public NaiveVoxelRenderer() {
        super();
        drawMode = GL_TRIANGLES;

        eyePos = new float[]{0, 0, 0};
        lightPos = new float[]{0, 1, 5};

        viewM = new float[16];
        modelM = new float[16];
        inverseModelM = new float[16];
        projM = new float[16];
        MVP = new float[16];
        temp = new float[16];
        Matrix.setIdentityM(viewM, 0);
        Matrix.setIdentityM(modelM, 0);
        Matrix.setIdentityM(projM, 0);
        Matrix.setIdentityM(MVP, 0);

        angle = 0f;
    }


    public void onSurfaceChanged(GL10 gl10, int w, int h) {
        super.onSurfaceChanged(gl10, w, h);
        float aspect = ((float) w) / ((float) (h == 0 ? 1 : h));
        float fovY = 45f;
        float fovX = 2 * (float) Math.atan(aspect * Math.tan(Math.toRadians(fovY) / 2));

        Matrix.perspectiveM(projM, 0, fovY, aspect, 0.1f, 1000f);

        float maxSize = Math.max(Math.max(modelVlyObject.getX(), modelVlyObject.getY()), modelVlyObject.getZ());
        maxSize *= (float) Math.sqrt(2);

        float cameraDistanceY = (maxSize / 2) / (float) Math.tan(Math.toRadians(45f) / 2);
        float cameraDistanceX = (maxSize / 2) / (float) Math.tan(fovX / 2);
        float cameraDistance = Math.max(cameraDistanceY, cameraDistanceX);

        maxZoom = cameraDistance;
        minZoom = 1.0f; //TODO: pensarlo meglio
        zoom = maxZoom;

        eyePos[2] = zoom;

        Matrix.setLookAtM(viewM, 0, eyePos[0], eyePos[1], eyePos[2],
                0, 0, 0,
                0, 1, 0);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void setContextAndSurface(Context context, GLSurfaceView surface) {
        super.setContextAndSurface(context, surface);

        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {

                zoom /= detector.getScaleFactor();
                zoom = Math.max(minZoom, Math.min(zoom, maxZoom));

                return true;
            }
        });

        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onScroll(@Nullable MotionEvent e1, @NonNull MotionEvent e2, float distanceX, float distanceY) {
                Log.v(TAG, "distanceX: " + distanceX);
                if (distanceX > 0)
                    angle -= 1f;
                else
                    angle += 1f;

                return true;
            }
        });


        surface.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            scaleDetector.onTouchEvent(event);
            return true;
        });
    }

    public void setAttributePointer(int vboIndex, Buffer buffer, AttributePointer pointer) {
        setAttributePointer(vboIndex, buffer, new AttributePointer[]{pointer});
    }

    public void setAttributePointer(int vboIndex, Buffer buffer, AttributePointer[] pointers) {
        int elementSize = buffer instanceof FloatBuffer ? Float.BYTES : Integer.BYTES;
        int elementType = buffer instanceof FloatBuffer ? GL_FLOAT : GL_INT;
        glBindVertexArray(VAO[0]);
            glBindBuffer(GL_ARRAY_BUFFER, VBO[vboIndex]);
                glBufferData(GL_ARRAY_BUFFER, elementSize * buffer.capacity(), buffer, GL_STATIC_DRAW);
                for (AttributePointer pointer : pointers) {
                    glVertexAttribPointer(pointer.getLocation(), pointer.getSize(), elementType, false, elementSize * pointer.getStride(), pointer.getOffset() * elementSize);
                    glEnableVertexAttribArray(pointer.getLocation());
                    if (pointer.isInstanced())
                        glVertexAttribDivisor(pointer.getLocation(), 1);
                }
            glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        super.onSurfaceCreated(gl10, eglConfig);

        loadShaders();
        loadCube();
        loadVoxelModel();

        Bitmap textureBitmap = generateTexture();

        float[] vertices = cubePlyObject.getVertices();
        int[] indices = cubePlyObject.getIndices();
        float[] offsets = generateOffsets();
        float[] textureIndices = generateTextureIndices(textureBitmap.getWidth());


        FloatBuffer vertexData = allocateFloatBuffer(vertices);
        IntBuffer indexData = allocateIntBuffer(indices);
        FloatBuffer offsetsData = allocateFloatBuffer(offsets);
        FloatBuffer textureIndicesData = allocateFloatBuffer(textureIndices);

        countFacesToElement = indices.length;

        VAO = new int[1];
        glGenVertexArrays(1, VAO, 0);

        VBO = new int[4];
        glGenBuffers(4, VBO, 0);

        setAttributePointer(0, vertexData,
                new AttributePointer[]{
                        new AttributePointer(VPOS_LOCATION, 3, 6, 0, false),
                        new AttributePointer(NORMALS_LOCATION, 3, 6, 3, false),
                });

        setAttributePointer(2, offsetsData,
                new AttributePointer(OFFSETS_LOCATION, 3, 3, 0, true));
        setAttributePointer(3, textureIndicesData,
                new AttributePointer(TEXTURE_INDICES, 1, 1, 0, true));


        glBindVertexArray(VAO[0]);
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, VBO[1]);
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, Integer.BYTES * indexData.capacity(), indexData, GL_STATIC_DRAW);
        glBindVertexArray(0);

        MVPloc = glGetUniformLocation(shaderHandle, "MVP");
        modelMloc = glGetUniformLocation(shaderHandle, "modelM");
        inverseModelMloc = glGetUniformLocation(shaderHandle, "inverseModelM");
        texUnit = glGetUniformLocation(shaderHandle, "tex");

        eyePosloc = glGetUniformLocation(shaderHandle, "eyePos");
        lightPosloc = glGetUniformLocation(shaderHandle, "lightPos");


        texObjId = new int[1];
        glGenTextures(1, texObjId, 0);
        glBindTexture(GL_TEXTURE_2D, texObjId[0]);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);


        GLUtils.texImage2D(GL_TEXTURE_2D, 0, textureBitmap, 0);
        glGenerateMipmap(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, 0);

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, texObjId[0]);
        glUseProgram(shaderHandle);
        glUniform1i(texUnit, 0);
        glUseProgram(0);
        glBindTexture(GL_TEXTURE_2D, 0);


        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        glFrontFace(GL_CCW);

        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);

    }

    private static double log2(double n) {
        return Math.log(n) / Math.log(2);
    }

    private int getImageWidth(int numColors) {
        return (int) Math.pow(2, Math.ceil(log2(numColors)));
    }

    private Bitmap generateTexture() {
        int[] colors = modelVlyObject.getColors();
        int numColors = modelVlyObject.getNumColors();
        int imageWidth = getImageWidth(numColors);

        int[] data = new int[imageWidth];
        for (int i = 0; i < numColors; i++) {
            data[i] = Color.rgb(
                    colors[i * 3],
                    colors[i * 3 + 1],
                    colors[i * 3 + 2]
            );
        }

        return Bitmap.createBitmap(data, imageWidth, 1, Bitmap.Config.ARGB_8888);
    }

    private float[] generateTextureIndices(float textWidth) {
        float[] indices = new float[modelVlyObject.getVoxelNum()];

        for (int i = 0; i < modelVlyObject.getVoxelNum(); i++) {
            indices[i] = modelVlyObject.getVoxelsPositionColorIndex()[i][3] / textWidth + (1 / (textWidth * 2));
        }

        return indices;
    }

    private float[] generateOffsets() {
        float[] offsets = new float[3 * modelVlyObject.getVoxelNum()];
        int[][] positions = modelVlyObject.getVoxelsPositionColorIndex();

        float marginX = (modelVlyObject.getX() - 1) * 0.5f;
        float marginY = (modelVlyObject.getY() - 1) * 0.5f;
        float marginZ = (modelVlyObject.getZ() - 1) * 0.5f;


        for (int i = 0; i < modelVlyObject.getVoxelNum(); i++) {
            int[] position = positions[i];
            offsets[i * 3] = -(position[0] - marginX);
            offsets[i * 3 + 1] = (position[1] - marginY);
            offsets[i * 3 + 2] = -(position[2] - marginZ);
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
            InputStream is = context.getAssets().open("models/dragon.vly");
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
            shaderHandle = ShaderCompiler.createProgram(isV, isF);
        } catch (IOException e) {
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

        eyePos[2] = zoom;

        Matrix.setLookAtM(viewM, 0, eyePos[0], eyePos[1], eyePos[2],
                0, 0, 0,
                0, 1, 0);

        Matrix.multiplyMM(temp, 0, projM, 0, viewM, 0);

        Matrix.setIdentityM(modelM, 0);
        Matrix.translateM(modelM, 0, 0, 0, 0);
        Matrix.rotateM(modelM, 0, angle, 0, 1, 0);

        Matrix.invertM(inverseModelM, 0, modelM, 0);

        Matrix.multiplyMM(MVP, 0, temp, 0, modelM, 0);

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, texObjId[0]);

        lightPos = eyePos;
        glUseProgram(shaderHandle);
        glBindVertexArray(VAO[0]);
        glUniformMatrix4fv(MVPloc, 1, false, MVP, 0);
        glUniformMatrix4fv(modelMloc, 1, false, modelM, 0);
        glUniformMatrix4fv(inverseModelMloc, 1, true, inverseModelM, 0);
        glUniform3fv(lightPosloc, 1, lightPos, 0);
        glUniform3fv(eyePosloc, 1, eyePos, 0);
        glDrawElementsInstanced(drawMode, countFacesToElement, GL_UNSIGNED_INT, 0, modelVlyObject.getVoxelNum());
        glBindVertexArray(0);
        glUseProgram(0);

    }

}
