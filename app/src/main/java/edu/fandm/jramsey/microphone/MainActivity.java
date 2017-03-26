package edu.fandm.jramsey.microphone;

import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;


import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import android.util.Log;
import android.view.MenuItem;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import android.widget.Toast;
import android.view.View;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;



public class MainActivity extends AppCompatActivity {

    private final static String TAG = MainActivity.class.getName();

    private static final int RECORDER_SAMPLERATE = 44100;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_8BIT;
    private static final int minBufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);


    private AudioRecord recorder = null;
    private AudioTrack audioTrack = null;

    //holds most recent recording
    byte[] recentRecording;

    //absolute path to the save folder
    String filePath;

    //name of individual file
    String timeStamp;

    //holds all files in directory
    public ArrayList<String> filessofar;


    boolean isRecording = false;
    boolean isSaved;
    boolean isPlaying = false;
    boolean canRecord;
    boolean canSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        writeExternalPermission();
        recordPermission();


        filePath = getAudioStorageDir("Microphone_Recordings").getAbsolutePath();

        //add old files
        File sdCardRoot = Environment.getExternalStorageDirectory();
        filessofar = new ArrayList<>();
        File yourDir = new File(sdCardRoot, "Microphone_Recordings");
        if (yourDir.listFiles() != null) {
            for (File f : yourDir.listFiles()) {
                if (f.isFile())
                    filessofar.add(f.getName());
            }
        }
    }

    public void onRecord(View v) {
        //if it is not recording and has permission to record, start animation and recording
        if (!isRecording) {
            if (canRecord) {
                recordingAnimation(true);
                recordAudio();
                isRecording=true;
            }else{
                //else ask permission
                recordPermission();
            }
        } else {
            //else it's already recording, so stop recording
            recordingAnimation(false);
            stopRecording();
            isRecording=false;
        }

    }

    public void playSound(byte[] thingToPlay) {
        //if it is already playing something, stop and release
        if (isPlaying){
            audioTrack.stop();
            audioTrack.release();
            isPlaying = false;
        }

        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, RECORDER_SAMPLERATE, AudioFormat.CHANNEL_OUT_MONO, RECORDER_AUDIO_ENCODING, thingToPlay.length, AudioTrack.MODE_STATIC);
        audioTrack.write(thingToPlay, 0, thingToPlay.length);
        audioTrack.play();
        isPlaying = true;
    }

    // animation
    private void recordingAnimation(boolean on) {
        View v = findViewById(R.id.curr_recording);
        if (on) {
            v.setVisibility(View.VISIBLE);
            Animation a = AnimationUtils.loadAnimation(this, R.anim.blink_anim);
            v.startAnimation(a);
        } else {
            v.setVisibility(View.INVISIBLE);
            v.clearAnimation();
        }
    }

    // menu for play, save, and view
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.play_rec:
                if (recentRecording != null){
                    playSound(recentRecording);
                } else{
                    Toast.makeText(this, "No recent recording", Toast.LENGTH_LONG).show();
                }
                return true;
            case R.id.save_rec:
                //if something has been recorded and you are allowed to save
                if (recentRecording != null && canSave){
                        writeAudioDataToFile(recentRecording);
                } else if (recentRecording==null){
                    //else they haven't recorded anything, tell them
                    Toast.makeText(this, "No recent recording", Toast.LENGTH_LONG).show();
                } else{
                    //else they need permission
                    writeExternalPermission();
                }
                return true;

            case R.id.view_rec:
                if (canSave){
                    //if they can save, open ViewActivity with list of files
                    loadSavedFiles();
                    Intent intent = new Intent(MainActivity.this, ViewActivity.class);
                    intent.putExtra("Recordings",filessofar);
                    startActivityForResult(intent, 100);
                    return true;

                } else{
                    writeExternalPermission();
                }

            default:
                return super.onOptionsItemSelected(item);
        }

    }

    //gets filename clicked on in the ViewActivity, opens the corresponding file and plays it
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 100 && resultCode == RESULT_OK){
            String savedFilename = data.getStringExtra("filename");
            String name= filePath+"/"+savedFilename;
            try{
                RandomAccessFile f = new RandomAccessFile(name, "r");
                byte[] b = new byte[(int)f.length()];
                f.readFully(b);
                playSound(b);
            } catch(FileNotFoundException e){
            } catch(IOException e){}
        }
    }


    public void recordAudio() {
        Runnable r = new MyRunnable();
        Thread t = new Thread(r, "My Thread's Name");
        t.start();
    }

    class MyRunnable implements Runnable {
        public void run() {
            if (!Thread.interrupted()) {
                    recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, RECORDER_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING, minBufferSize);
                    recorder.startRecording();
                    record();
            }
        }
    }

    private byte[] record() {
        //if its already playing, stop the sound before recording
        if (isPlaying){
            audioTrack.stop();
            audioTrack.release();
            isPlaying = false;
        }

        timeStamp = new SimpleDateFormat("yyyy'_'MM'_'dd'-'hh':'mm':'ss'.pcm'", Locale.US).format(new Date());
        isSaved=false;

        int arrayIdx;
        ArrayList<Byte> recording = new ArrayList<>(88200);
        byte[] bData = new byte[minBufferSize];
        while (isRecording) {
            // gets the voice output from microphone to byte format
            arrayIdx = recorder.read(bData, 0, minBufferSize);
            for (int i = 0; i < arrayIdx; i++) {
                recording.add(bData[i]);
            }
        }

        //copy ArrayList recording to byte[] recentRecording
        recentRecording = new byte[recording.size()];
        for (int i = 0; i < recording.size(); i++) {
            recentRecording[i] = recording.get(i);
        }
        return recentRecording;
    }

    //saves audio file by timestamp
    private void writeAudioDataToFile(byte[] fileToWrite) {
            if (isSaved) {
                Toast.makeText(this, "File already saved", Toast.LENGTH_LONG).show();
            } else {
                try {
                    filessofar.add(timeStamp);
                    FileOutputStream os = new FileOutputStream(filePath + "/" + timeStamp);
                    os.write(fileToWrite, 0, fileToWrite.length);
                    os.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "File Not Found Exception!", Toast.LENGTH_LONG).show();
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "File IO Exception!", Toast.LENGTH_LONG).show();
                }
                isSaved = true;
            }
    }

    private void stopRecording() {
        // stops the recording activity
        if (null != recorder) {
            isRecording = false;
            recorder.stop();
            recorder.release();
            recorder = null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isPlaying){
            audioTrack.stop();
            audioTrack.release();
            isPlaying = false;
        }
        if (isRecording) {
            stopRecording();
            recordingAnimation(false);
            isRecording =false;
        }
        if (recorder != null) {
            //recorder.stop();
            recorder.release();
            System.out.println("Mic released");
        }
    }




      /*
    * PERMISSIONS FUNCTIONS BELOW
     */

    public void recordPermission(){
        //ask for permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
        } else{
            canRecord = true;
        }
    }

    //code from https://developer.android.com/training/permissions/requesting.html
    public void writeExternalPermission(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);
        } else{
            canSave = true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    canRecord =true;
                } else {
                    canRecord=false;
                    // permission denied, boo! Disable the functionality that depends on this permission.
                }
            }
            case 2: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (isExternalStorageWritable()) {
                        File file = getAudioStorageDir("Microphone_Recordings");
                        canSave= true;
                        loadSavedFiles();
                    }
                } else {
                    canSave = false;
                }
            }
        }
    }

    //loads all files in the directory to filessofar (so that they will all appear in ViewActivity)
    public void loadSavedFiles(){
        File sdCardRoot = Environment.getExternalStorageDirectory();
        filessofar = new ArrayList<>();
        File yourDir = new File(sdCardRoot, "Microphone_Recordings");
        if (yourDir.listFiles() != null) {
            for (File f : yourDir.listFiles()) {
                if (f.isFile())
                    filessofar.add(f.getName());
            }
        }
    }

    //gets the directory to store files in
    public File getAudioStorageDir(String folderName){
        File file = new File(Environment.getExternalStorageDirectory(), folderName);
        if (!file.mkdirs()) {
            Log.e(TAG, "Directory not created");
        }
        return file;
    }

    //determines if external storage is available
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

}