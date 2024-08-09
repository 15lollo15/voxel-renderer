package com.example.voxelrenderer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
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

public class VoxelRenderer extends BasicRenderer {
    public static final float FOV_Y = 45f;
    public static final float Z_NEAR = .1f;
    public static final float Z_FAR = 1000f;
    public static final float MAX_ZOOM_MARGIN_FACTOR = 1.1f;
    public static final float MIN_ZOOM_RATIO = 0.3f;


    public static final float SWIPE_SPEED = 1f;
    public static final float TAP_SPEED = 30f;

    public enum BuffersIndices {
        VERTICES_NORMALS,
        INDICES,
        OFFSETS,
        TEXTURE_INDICES
    }

    private float fovX;

    private PlyObject cubePlyObject;
    private VlyObject modelVlyObject;
    private float angle;

    private int shaderHandle;
    private int[] VAO;
    private int[] VBO;
    private int countFacesToElement;
    private int[] texObjId;
    private float modelMaxSize;

    private final float[] eyePos;
    private float[] lightPos;

    private final Transformations transformations;
    private UniformLocations uniformLocations;

    private final int drawMode;

    private float maxZoom;
    private float minZoom;

    public VoxelRenderer() {
        super();
        drawMode = GL_TRIANGLES;

        eyePos = new float[3];
        lightPos = new float[3];

        transformations = new Transformations();

        angle = 0f;
    }


    private float getMinimumDistanceCamera(float modelMaxSize) {
        float cameraDistanceY = (modelMaxSize / 2) / (float) Math.tan(Math.toRadians(FOV_Y) / 2);
        float cameraDistanceX = (modelMaxSize / 2) / (float) Math.tan(fovX / 2);

        return Math.max(cameraDistanceY, cameraDistanceX);
    }

    private void computeMaxSize() {
        modelMaxSize = Math.max(Math.max(modelVlyObject.getX(), modelVlyObject.getY()), modelVlyObject.getZ());
        modelMaxSize *= (float) Math.sqrt(2);
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int w, int h) {
        super.onSurfaceChanged(gl10, w, h);
        float aspect = ((float) w) / ((float) (h == 0 ? 1 : h));
        fovX = 2 * (float) Math.atan(aspect * Math.tan(Math.toRadians(FOV_Y) / 2));

        maxZoom = getMinimumDistanceCamera(modelMaxSize) * MAX_ZOOM_MARGIN_FACTOR;
        minZoom = maxZoom * MIN_ZOOM_RATIO;
        eyePos[2] = maxZoom;

        transformations.setProjectionMatrix(FOV_Y, aspect, Z_NEAR, Z_FAR);
        transformations.setViewMatrix(eyePos);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void setContextAndSurface(Context context, GLSurfaceView surface) {
        super.setContextAndSurface(context, surface);

        ScaleGestureDetector scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(@NonNull ScaleGestureDetector detector) {
                eyePos[2] /= detector.getScaleFactor();
                eyePos[2] = Math.max(minZoom, Math.min(eyePos[2], maxZoom));

                return true;
            }
        });

        GestureDetector gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onScroll(@Nullable MotionEvent e1, @NonNull MotionEvent e2, float distanceX, float distanceY) {
                if (distanceX > 0)
                    angle -= SWIPE_SPEED;
                else
                    angle += SWIPE_SPEED;

                return true;
            }

            @Override
            public boolean onSingleTapUp(@NonNull MotionEvent e) {
                if (e.getX() > surface.getWidth() / 2f)
                    angle += TAP_SPEED;
                else
                    angle -= TAP_SPEED;
                return true;
            }
        });


        surface.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            scaleDetector.onTouchEvent(event);
            return true;
        });
    }

    private void setAttributePointer(BuffersIndices vboIndex, float[] floatArray, AttributePointer[] pointers) {
        FloatBuffer buffer = allocateFloatBuffer(floatArray);
        setAttributePointer(vboIndex, buffer, pointers);
    }

    private void setAttributePointer(BuffersIndices vboIndex, Buffer buffer, AttributePointer[] pointers) {
        int elementSize = buffer instanceof FloatBuffer ? Float.BYTES : Integer.BYTES;
        int elementType = buffer instanceof FloatBuffer ? GL_FLOAT : GL_INT;
        glBindVertexArray(VAO[0]);
        glBindBuffer(GL_ARRAY_BUFFER, VBO[vboIndex.ordinal()]);
        glBufferData(GL_ARRAY_BUFFER, elementSize * buffer.capacity(), buffer, GL_STATIC_DRAW);
        for (AttributePointer pointer : pointers) {
            glVertexAttribPointer(pointer.getLocation().getValue(), pointer.getSize(), elementType, false, elementSize * pointer.getStride(), pointer.getOffset() * elementSize);
            glEnableVertexAttribArray(pointer.getLocation().getValue());
            if (pointer.isInstanced())
                glVertexAttribDivisor(pointer.getLocation().getValue(), 1);
        }
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    private void setIndices(int vboIndex, int[] indices) {
        IntBuffer buffer = allocateIntBuffer(indices);
        glBindVertexArray(VAO[0]);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, VBO[vboIndex]);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, Integer.BYTES * buffer.capacity(), buffer, GL_STATIC_DRAW);
        glBindVertexArray(0);
    }

    private void setupTexture(Bitmap textureBitmap) {
        glBindTexture(GL_TEXTURE_2D, texObjId[0]);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            GLUtils.texImage2D(GL_TEXTURE_2D, 0, textureBitmap, 0);
            glGenerateMipmap(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, 0);

        glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, texObjId[0]);
            glUseProgram(shaderHandle);
                glUniform1i(uniformLocations.getTexLocation(), 0);
            glUseProgram(0);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    private void setCapabilities() {
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        glFrontFace(GL_CCW);

        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        super.onSurfaceCreated(gl10, eglConfig);

        loadShaders();
        uniformLocations = UniformLocations.getInstance(shaderHandle);

        loadCube();
        loadVoxelModel();

        Bitmap textureBitmap = modelVlyObject.generateTexture();

        countFacesToElement = cubePlyObject.getIndices().length;

        VAO = new int[1];
        glGenVertexArrays(1, VAO, 0);

        VBO = new int[BuffersIndices.values().length];
        glGenBuffers(BuffersIndices.values().length, VBO, 0);

        setAttributePointer(BuffersIndices.VERTICES_NORMALS, cubePlyObject.getVertices(),
                new AttributePointer[]{
                        new AttributePointer(ShaderLocations.VPOS_LOCATION, 3, 6, 0, false),
                        new AttributePointer(ShaderLocations.NORMALS_LOCATION, 3, 6, 3, false),
                });

        setAttributePointer(BuffersIndices.OFFSETS, modelVlyObject.getOffsets(),
                new AttributePointer[]{
                        new AttributePointer(ShaderLocations.OFFSETS_LOCATION, 3, 3, 0, true)
                });
        setAttributePointer(BuffersIndices.TEXTURE_INDICES, modelVlyObject.generateTextureIndices(),
                new AttributePointer[]{
                        new AttributePointer(ShaderLocations.TEXTURE_INDICES, 1, 1, 0, true
                )});

        setIndices(BuffersIndices.INDICES.ordinal(), cubePlyObject.getIndices());

        texObjId = new int[1];
        glGenTextures(1, texObjId, 0);
        setupTexture(textureBitmap);
        textureBitmap.recycle();


        setCapabilities();
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
            InputStream is = context.getAssets().open("models/christmas.vly");
            modelVlyObject = new VlyObject(is);
            modelVlyObject.parse();
            computeMaxSize();
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

        transformations.setViewMatrix(eyePos);
        transformations.setModelMatrix(0, 0, 0, angle);


        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, texObjId[0]);

        lightPos = eyePos;
        glUseProgram(shaderHandle);
            glBindVertexArray(VAO[0]);
                glUniformMatrix4fv(uniformLocations.getMVPLocation(), 1, false, transformations.getMVP(), 0);
                glUniformMatrix4fv(uniformLocations.getModelMatrixLocation(), 1, false, transformations.getModelMatrix(), 0);
                glUniformMatrix4fv(uniformLocations.getInverseModelMatrixLocation(), 1, true, transformations.getInvertedModelMatrix(), 0);
                glUniform3fv(uniformLocations.getLightPositionLocation(), 1, lightPos, 0);
                glUniform3fv(uniformLocations.getEyePositionLocation(), 1, eyePos, 0);
                glDrawElementsInstanced(drawMode, countFacesToElement, GL_UNSIGNED_INT, 0, modelVlyObject.getVoxelNum());
            glBindVertexArray(0);
        glUseProgram(0);

    }

}
