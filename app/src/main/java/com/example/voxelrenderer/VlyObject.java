package com.example.voxelrenderer;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Iterator;

public class VlyObject {

    private static final String TAG = "VLY_OBJECT";
    private final InputStream inputStream;
    private int x;
    private int y;
    private int z;
    private int voxelNum;
    private int[][] voxelsPositionColorIndex;
    private int[] colors;
    private float[] offsets;

    private int numColors;

    public VlyObject(InputStream inputStream) {
        this.inputStream = inputStream;
        numColors = 0;
    }

    public void parse() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        Iterator<String> it = reader.lines().iterator();

        readGridSize(it.next());
        readVoxelNum(it.next());

        readVoxelPositionColorIndex(it);
        readColors(it);

        reader.close();
        inputStream.close();

        generateOffsets();
    }

    private void readColors(Iterator<String> it) {
        colors = new int[numColors * 3];
        while (it.hasNext()) {
            String colorsIndexesValuesString = it.next();
            String[] splitted = colorsIndexesValuesString.split(" ");
            int index = Integer.parseInt(splitted[0]);
            int r = Integer.parseInt(splitted[1]);
            int g = Integer.parseInt(splitted[2]);
            int b = Integer.parseInt(splitted[3]);

            colors[index * 3] = r;
            colors[index * 3 + 1] = g;
            colors[index * 3 + 2] = b;
        }
    }

    private void readVoxelPositionColorIndex(Iterator<String> it) {
        voxelsPositionColorIndex = new int[voxelNum][];
        for (int i = 0; i < voxelNum; i++) {
            String voxelPositionColorString = it.next();

            int[] positions = Arrays.stream(voxelPositionColorString.split(" ")).
                    mapToInt(Integer::valueOf).toArray();

            voxelsPositionColorIndex[i] = new int[4];

            voxelsPositionColorIndex[i][0] = positions[0];
            voxelsPositionColorIndex[i][1] = positions[2];
            voxelsPositionColorIndex[i][2] = positions[1];
            voxelsPositionColorIndex[i][3] = positions[3];

            numColors = Math.max(numColors, voxelsPositionColorIndex[i][3] + 1);
        }
        Log.v(TAG, "numcolors: " + numColors);
    }

    private void readVoxelNum(String voxelNumString) {
        String value = voxelNumString.split(":")[1].trim();
        voxelNum = Integer.parseInt(value);
    }

    private void readGridSize(String gridSizeString) {
        String value = gridSizeString.split(":")[1].trim();
        int[] sizes = Arrays.stream(value.split(" ")).mapToInt(Integer::valueOf).toArray();
        x = sizes[0];
        y = sizes[2];
        z = sizes[1];
    }

    private void generateOffsets() {
        offsets = new float[3 * voxelNum];

        float marginX = (x - 1) * 0.5f;
        float marginY = (y - 1) * 0.5f;
        float marginZ = (z - 1) * 0.5f;


        for (int i = 0; i < voxelNum; i++) {
            int[] position = voxelsPositionColorIndex[i];
            offsets[i * 3] = -(position[0] - marginX);
            offsets[i * 3 + 1] = (position[1] - marginY);
            offsets[i * 3 + 2] = -(position[2] - marginZ);
        }
    }

    private static double log2(double n) {
        return Math.log(n) / Math.log(2);
    }

    private int getImageWidth(int numColors) {
        return (int) Math.pow(2, Math.ceil(log2(numColors)));
    }

    public Bitmap generateTexture() {
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

    public float[] generateTextureIndices() {
        float textWidth = getImageWidth(numColors);
        float[] indices = new float[voxelNum];

        for (int i = 0; i < voxelNum; i++) {
            indices[i] = voxelsPositionColorIndex[i][3] / textWidth + (1 / (textWidth * 2));
        }

        return indices;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public int getVoxelNum() {
        return voxelNum;
    }

    public int[][] getVoxelsPositionColorIndex() {
        return voxelsPositionColorIndex;
    }

    public int[] getColors() {
        return colors;
    }

    public int getNumColors() {
        return numColors;
    }

    public float[] getOffsets() {
        return offsets;
    }
}
