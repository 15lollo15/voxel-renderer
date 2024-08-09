package com.example.voxelrenderer;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.Arrays;

public class ModelChooserActivity extends AppCompatActivity {
    public static final String TAG = "MODEL_CHOOSER";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_model_chooser);

        String[] assetsNames = null;

        try {
            assetsNames = getAssets().list("models");
        } catch (IOException e) {
            Log.e(TAG, "Error in fetching assets");
            System.exit(1);
        }
        for (String name : assetsNames)
            Log.v(TAG, name);
        String[] vlyModels = Arrays.stream(assetsNames).filter(name -> name.contains(".vly")).toArray(String[]::new);

        ArrayAdapter<String> adapter= new ArrayAdapter<>(this, R.layout.row, vlyModels);
        ListView listView = findViewById(R.id.listview);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((adapterView, view, i, l) -> {
            String modelName = ((TextView)view).getText().toString();
            Intent intent = new Intent(this, ModelViewerActivity.class);
            intent.putExtra("modelName", modelName);
            startActivity(intent);
        });

    }
}