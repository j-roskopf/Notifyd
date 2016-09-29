package com.joe.notifyd;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import com.joe.notifyd.Util.Constants;
import com.wnafee.vector.MorphButton;

import java.io.File;
import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.pedant.SweetAlert.SweetAlertDialog;
import io.gresse.hugo.vumeterlibrary.VuMeterView;

public class AudioRecordingActivity extends AppCompatActivity {

    /**
     * FINAL
     */

    final float DEFAULT_OFF_ALPHA = .5f;
    final float DEFAULT_ON_ALPHA = 1.0f;

    /**
     * UI
     */

    @BindView(R.id.vumeter)
    VuMeterView vuMeterView;

    @BindView(R.id.recordButton)
    ImageView recordButton;

    @BindView(R.id.stopButton)
    ImageView stopButton;


    @BindView(R.id.playPauseButton)
    com.wnafee.vector.MorphButton playPauseButton;

    /**
     * NON UI
     */

    //are we currently recording
    private boolean isPlaying;

    private MediaRecorder mediaRecorder;

    private MediaPlayer mediaPlayer;

    private int currentId;

    /**
     * file location for current media
     */
    private String filename;

    /**
     * if the user saved the file, don't delete the current session onPause
     */
    boolean didUserSave;
    boolean alreadyExists;


    /**
     * we need to keep track if the user actually touched the play/pause button or if me manually calling stateChange is what triggered the onStateChanged method
     */

    private boolean userTouched;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_recording);

        ButterKnife.bind(this);

        currentId = getIntent().getIntExtra(Constants.ID_KEY, -1);

        Log.d("D", "audioRecordingDebug with currentId = " + currentId);

        if (currentId == -1) {
            displayGenericErrorMessageWithTitle("An error has occured");
            return;
        }

        initVars();

        setupPlayPauseButton();

        setupRecordButton();

        setupVisualizer();

        setupStopButton();

        String file = getIntent().getStringExtra(Constants.ID_AUDIO_FILE);

        if(file != null && !file.equals("") && new File(file).exists()){
            //must already exist. use it!

            alreadyExists = true;

            filename = new File(file).getAbsolutePath();

            setupExistingUI();
        }else{
            File currentFile = new File(getFilesDir(), currentId + "_recording.3gp");

            alreadyExists = false;

            if (currentFile.exists()) {
                //setup existing UI, file exists
                filename = currentFile.getAbsolutePath();
                setupExistingUI();
            } else {
                setupDefaultUI();
            }
        }


    }


    /**
     * initialized variables
     */
    private void initVars() {
        isPlaying = false;
        filename = "";
        userTouched = false;
        didUserSave = false;
    }

    /**
     * setup / customize EQ
     */
    private void setupVisualizer() {
        //this is the only way I could get the EQ to start minimized
        /*Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                vuMeterView.stop(true);
            }
        }, 1000);*/

    }

    /**
     * setup logic for record button
     */
    private void setupRecordButton() {
        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setRecordingUI();

                startRecording();
            }
        });
    }

    /**
     * start recording!
     */
    private void startRecording() {

        File file = new File(getFilesDir(), currentId + "_recording.3gp");

        filename = file.getAbsolutePath();

        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setOutputFile(filename);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            mediaRecorder.prepare();
            mediaRecorder.start();

            vuMeterView.resume(true);
        } catch (IOException e) {
            displayGenericErrorMessageWithTitle("Something went wrong. Sorry!");
            setupDefaultUI();
        }


    }

    /**
     * setup logic for stop button
     */
    private void setupStopButton() {
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //save file

                //set stopped recording ui
                setupStoppedUI();

                //stop EQ
                vuMeterView.stop(true);

                //reset pause / play button
                playPauseButton.setState(MorphButton.MorphState.START);

                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;

                if(mediaPlayer != null){
                    //if this is the first recording, mediaPlayer will be null
                    mediaPlayer.stop();
                    mediaPlayer.release();
                    mediaPlayer = null;
                }

            }
        });
    }

    /**
     * sets the UI state of the buttons during recording
     */
    private void setRecordingUI() {

        //alpha record button and make it unclickable so the user doesn't click it again
        recordButton.setAlpha(DEFAULT_OFF_ALPHA);
        recordButton.setClickable(false);

        //set pause play button to be pause so the user can pause
        playPauseButton.setAlpha(DEFAULT_OFF_ALPHA);
        playPauseButton.setClickable(false);
        playPauseButton.setState(MorphButton.MorphState.START);


        //set alpha of stop button to normal and enable
        stopButton.setAlpha(DEFAULT_ON_ALPHA);
        stopButton.setClickable(true);
    }

    /**
     * enables default button states
     */
    private void setupDefaultUI() {
        //set clickable and full alpha for record button
        recordButton.setAlpha(DEFAULT_ON_ALPHA);
        recordButton.setClickable(true);

        //disable pause / play button
        playPauseButton.setAlpha(DEFAULT_OFF_ALPHA);
        playPauseButton.setClickable(false);

        //disable stop button
        stopButton.setAlpha(DEFAULT_OFF_ALPHA);
        stopButton.setClickable(false);
    }

    /**
     * enables button states when opening an existing file
     */
    private void setupExistingUI() {
        //set clickable and full alpha for record button
        recordButton.setAlpha(DEFAULT_ON_ALPHA);
        recordButton.setClickable(true);

        //disable pause / play button
        playPauseButton.setAlpha(DEFAULT_ON_ALPHA);
        playPauseButton.setClickable(true);

        //disable stop button
        stopButton.setAlpha(DEFAULT_OFF_ALPHA);
        stopButton.setClickable(false);

    }

    /**
     * sets the UI state of the buttons during stopped
     */
    private void setupStoppedUI() {

        //alpha record button and make it unclickable so the user doesn't click it again
        recordButton.setAlpha(DEFAULT_ON_ALPHA);
        recordButton.setClickable(true);

        //set pause play button to be pause so the user can pause
        playPauseButton.setAlpha(DEFAULT_ON_ALPHA);
        playPauseButton.setClickable(true);
        playPauseButton.setState(MorphButton.MorphState.START);

        //set alpha of stop button to normal and enable
        stopButton.setAlpha(DEFAULT_OFF_ALPHA);
        stopButton.setClickable(false);
    }


    /**
     * adds on click for play / pause button
     */
    private void setupPlayPauseButton() {


        playPauseButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                userTouched = true;
                return false;
            }
        });


        playPauseButton.setOnStateChangedListener(new MorphButton.OnStateChangedListener() {
            @Override
            public void onStateChanged(MorphButton.MorphState changedTo, boolean isAnimating) {
                Log.d("D", "playPauseButtonDebug onStateChanged " + isAnimating);
                if (userTouched) {
                    userTouched = false;

                    if (mediaPlayer == null) {
                        if (!filename.equals("")) {
                            try {
                                mediaPlayer = new MediaPlayer();
                                mediaPlayer.setDataSource(filename);
                                mediaPlayer.prepare();
                                mediaPlayer.start();

                                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                    @Override
                                    public void onCompletion(MediaPlayer mp) {
                                        //reset button
                                        playPauseButton.setState(MorphButton.MorphState.START);
                                        vuMeterView.stop(true);
                                    }
                                });
                            } catch (Exception e) {
                                Log.d("D", "playException with e = " + e.getMessage());
                                displayGenericErrorMessageWithTitle("Something went wrong while trying to play");
                            }
                        } else {
                            displayGenericErrorMessageWithTitle("Please record something first");
                        }
                    } else {
                        if (mediaPlayer.isPlaying()) {
                            vuMeterView.stop(true);
                            mediaPlayer.pause();
                        } else {
                            vuMeterView.resume(true);
                            mediaPlayer.start();
                        }
                    }
                }
            }
        });
    }

    /**
     * displays generic error message
     *
     * @param title
     */
    private void displayGenericErrorMessageWithTitle(String title) {
        new SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
                .setTitleText("Warning")
                .setContentText(title)
                .setConfirmText("Okay")
                .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sDialog) {
                        sDialog.dismissWithAnimation();
                    }
                })
                .show();
    }

    /**
     * saves current audio file
     */
    private void save(){

        if(currentId == -1){
            new SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
                    .setTitleText("Warning")
                    .setContentText("Something went wrong.")
                    .setConfirmText("Okay")
                    .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                        @Override
                        public void onClick(SweetAlertDialog sDialog) {
                            sDialog.dismissWithAnimation();
                            finish();
                        }
                    })
                    .show();
        }

        if(filename.equals("")){
            displayGenericErrorMessageWithTitle("Please record something first");
            return;
        }

        didUserSave = true;

        new SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                .setTitleText("Success")
                .setContentText("Saved successfully with path " + filename)
                .setConfirmText("Okay")
                .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sDialog) {
                        sDialog.dismissWithAnimation();
                        Intent returnIntent = new Intent();
                        returnIntent.putExtra(Constants.ID_ACTIVITY_RESULT_GOOD_KEY,currentId);
                        if(filename != null && !filename.equals("")){
                            returnIntent.putExtra(Constants.ID_AUDIO,filename);
                        }
                        setResult(Activity.RESULT_OK,returnIntent);
                        finish();
                    }
                })
                .show();

    }

    /**
     * displays the delete message
     */
    private void showDeleteMessage(){
        new SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
                .setTitleText("Are you sure?")
                .setContentText("Won't be able to recover this recording!")
                .setConfirmText("Yes, delete it!")
                .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sDialog) {
                        sDialog.dismissWithAnimation();
                        Intent returnIntent = new Intent();
                        returnIntent.putExtra(Constants.ID_ACTIVITY_RESULT_GOOD_KEY,currentId);

                        filename = "";
                        returnIntent.putExtra(Constants.ID_AUDIO,filename);

                        File thisSession =  new File(filename);
                        if(thisSession.exists()){
                            thisSession.delete();
                        }

                        setResult(Activity.RESULT_OK,returnIntent);
                        finish();
                    }
                })
                .show();
    }



    @Override
    public void onPause() {
        super.onPause();
        if (mediaRecorder != null) {
            mediaRecorder.release();
            mediaRecorder = null;
        }

        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }

        if(!didUserSave && !alreadyExists){
            //if the user didn't save it and we are pausing, delete the current session recording
            File thisSession =  new File(filename);
            if(thisSession.exists()){
                thisSession.delete();
            }
        }



    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.audio_recording_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_delete:
                showDeleteMessage();
                return true;

            case R.id.action_save:
                save();
                return true;


            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }
}
