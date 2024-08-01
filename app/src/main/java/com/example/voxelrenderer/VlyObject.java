package com.example.voxelrenderer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

public class VlyObject {

    private static final String TAG = "VLY_OBJECT";
    private final InputStream inputStream;
    private int x;
    private int y;
    private int z;
    private int voxelNum;
    private int[][] voxelsPositionColorIndex;
    private Map<Integer, int[]> colors;

    public VlyObject(InputStream inputStream) {
        this.inputStream = inputStream;
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
    }

    private void readColors(Iterator<String> it) {
        colors = new TreeMap<>();
        while (it.hasNext()) {
            String colorsIndexesValuesString = it.next();

            String[] splitted = colorsIndexesValuesString.split(" ");
            int index = Integer.parseInt(splitted[0]);
            int r = Integer.parseInt(splitted[1]);
            int g = Integer.parseInt(splitted[2]);
            int b = Integer.parseInt(splitted[3]);

            colors.put(index, new int[]{r, g, b});
        }
    }

    private void readVoxelPositionColorIndex(Iterator<String> it) {
        voxelsPositionColorIndex = new int[voxelNum][];
        for (int i = 0; i < voxelNum; i++) {
            String voxelPositionColorString = it.next();

            voxelsPositionColorIndex[i] = Arrays.stream(voxelPositionColorString.split(" ")).
                    mapToInt(Integer::valueOf).toArray();
        }
    }

    private void readVoxelNum(String voxelNumString) {
        String value = voxelNumString.split(":")[1].trim();
        voxelNum = Integer.parseInt(value);
    }

    private void readGridSize(String gridSizeString) {
        String value = gridSizeString.split(":")[1].trim();
        int[] sizes = Arrays.stream(value.split(" ")).mapToInt(Integer::valueOf).toArray();
        x = sizes[0];
        y = sizes[1];
        z = sizes[2];
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

    public Map<Integer, int[]> getColors() {
        return colors;
    }
}
