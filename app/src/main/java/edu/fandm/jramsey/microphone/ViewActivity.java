package edu.fandm.jramsey.microphone;

import android.content.Intent;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.File;
import java.util.ArrayList;

public class ViewActivity extends AppCompatActivity {
    ListView myList;
    ArrayAdapter<String> myAdapter;

    //filessofar from MainActivity
    ArrayList<String> newRec;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view);

        Bundle bundle = getIntent().getExtras();
        newRec = bundle.getStringArrayList("Recordings");

        myAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, newRec);

        myList=(ListView) findViewById(R.id.view_list);
        myList.setAdapter(myAdapter);

        //if one of the files is clicked on to be played, send filename to MainActivity to call playSound
        myList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                String filename = (String)myList.getItemAtPosition(position);
                Intent intent = new Intent();
                intent.putExtra("filename", filename);
                setResult(RESULT_OK, intent);
                finish();
            }
        });
    }

    //creates menu with delete all option
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.viewmenu, menu);
        return true;
    }


    //if delete all is clicked, delete all files in Microphone_Recordings
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.delete_rec:

                File file = new File(Environment.getExternalStorageDirectory(), "Microphone_Recordings");
                String filePath =  file.getAbsolutePath();
                File sdCardRoot = Environment.getExternalStorageDirectory();
                File yourDir = new File(sdCardRoot, "Microphone_Recordings");
                if (yourDir.listFiles() != null) {
                    for (File f : yourDir.listFiles()) {
                        if (f.isFile())
                            f.delete();
                    }
                }
                myAdapter.clear();
                myList.deferNotifyDataSetChanged();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }
}