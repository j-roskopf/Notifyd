package com.joe.notifyd;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.CalendarContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.codetroopers.betterpickers.calendardatepicker.CalendarDatePickerDialogFragment;
import com.codetroopers.betterpickers.radialtimepicker.RadialTimePickerDialogFragment;
import com.joe.notifyd.RealmObjects.Notification;
import com.joe.notifyd.Util.Constants;
import com.joe.notifyd.Util.Helper;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Random;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.pedant.SweetAlert.SweetAlertDialog;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;

public class MainActivity extends AppCompatActivity implements CalendarDatePickerDialogFragment.OnDateSetListener, RadialTimePickerDialogFragment.OnTimeSetListener {

    /**
     * FINAL
     */
    private static final String TEXT_KEY = "text";
    private static final String PERSISTENCE_KEY = "persistence";
    private static final String CLOSE_APP_KEY = "closeApp";
    private static final int DRAWING_ACTIVITY_RESULT = 109;
    private static final int AUDIO_ACTIVITY_RESULT = 119;
    private static final String FRAG_TAG_DATE_PICKER = "fragment_date_picker";
    private static final String FRAG_TAG_TIME_PICKER = "fragment_time_picker";
    private static final int AUDIO_PERMISSION = 8690;


    /**
     * UI
     */

    @BindView(R.id.notificationInput)
    com.rengwuxian.materialedittext.MaterialEditText inputText;

    @BindView(R.id.drawingButton)
    Button drawingButton;

    @BindView(R.id.calendarButton)
    Button calendarButton;

    @BindView(R.id.audioButton)
    Button audioButton;

    @BindView(R.id.persitentCheckbox)
    CheckBox persitentCheckbox;

    @BindView(R.id.closeAppCheckbox)
    CheckBox closeAppCheckbox;

    @BindView(R.id.feedbackButton)
    Button feedbackButton;

    @BindView(R.id.imageDisplay)
    ImageView imageViewDisplay;

    MenuItem deleteItem;

    /**
     * NON UI
     */

    private Helper helper;

    private SharedPreferences sharedPreferences;

    private NotificationManager notificationManager;

    private Random idGenerator;

    private Realm realm;

    private Context context;

    //image from drawingActivity
    private byte[] image;

    private String audioFilename;

    /**
     * used to pass values to calendar
     */
    int yearForCalendar;
    int monthForCalendar;
    int dayForCalendar;
    int hourForCalendar;
    int minuteForCalendar;


    /**
     * if loading previous message, currentId is set to > -1
     */
    private int currentId = -1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        initVars();

        populateFields();

        setupCloseCheckbox();

        setupDrawingButton();

        setupCalendarButton();

        setupAudioButton();

        setupPersistenceCheckBox();

        setupFeedbackButton();

    }

    /**
     * setup logic for feedback button
     */
    private void setupFeedbackButton() {
        feedbackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getApplicationContext(),FeedbackActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        Log.d("D","onActivityResultDebug request and result code = " + requestCode + " " + resultCode);

        if (requestCode == DRAWING_ACTIVITY_RESULT) {
            if(resultCode == Activity.RESULT_OK){
                int returnedId = data.getIntExtra(Constants.ID_ACTIVITY_RESULT_GOOD_KEY, -1);
                currentId = returnedId;

                image = data.getByteArrayExtra(Constants.ID_IMAGE);

                //set image
                if(image != null){
                    Bitmap bitmap = BitmapFactory.decodeByteArray(image, 0, image.length);
                    imageViewDisplay.setVisibility(View.VISIBLE);
                    setImageHeight();
                    imageViewDisplay.setImageBitmap(bitmap);
                }

                Log.d("D","onActivityResultDebug with returnedId = " + returnedId);
            }
        }else if (requestCode == AUDIO_ACTIVITY_RESULT) {
            if(resultCode == Activity.RESULT_OK){
                int returnedId = data.getIntExtra(Constants.ID_ACTIVITY_RESULT_GOOD_KEY, -1);
                currentId = returnedId;

                audioFilename = data.getStringExtra(Constants.ID_AUDIO);

                Log.d("D","onActivityResultDebug with returnedId = " + returnedId + " and filename = " + audioFilename);
            }
        }

    }

    /**
     * Init variables
     */
    private void initVars() {
        helper = new Helper(this);
        sharedPreferences = this.getSharedPreferences(
                getResources().getString(R.string.app_name), Context.MODE_PRIVATE);
        notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        idGenerator = new Random();

        audioFilename = "";


        // The Realm file will be located in package's "files" directory.
        RealmConfiguration realmConfig = new RealmConfiguration.Builder(this).build();
        Realm.setDefaultConfiguration(realmConfig);

        realm = Realm.getDefaultInstance();

        context = this;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        realm.close();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_menu, menu);

        deleteItem = menu.findItem(R.id.action_delete);

        if(currentId != -1){
            deleteItem.setVisible(true);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_delete:
                delete();
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

    /**
     * determines if persistence checkbox should be checked or not
     */
    private void setupPersistenceCheckBox(){
        if(sharedPreferences.getBoolean(PERSISTENCE_KEY,false)){
            persitentCheckbox.setChecked(true);
        }
    }

    /**
     * check for audio permission in onclick
     */
    private void setupAudioButton(){
        audioButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkAudioPermission();
            }
        });
    }

    /**
     * Asks user for audio permission
     */
    private void checkAudioPermission() {

        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.RECORD_AUDIO)) {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

                new SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
                        .setTitleText("Warning")
                        .setContentText("The audio recording permission is required to add audio to your note. Request again?")
                        .setConfirmText("Okay")
                        .setCancelText("Nope")
                        .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                            @Override
                            public void onClick(SweetAlertDialog sDialog) {
                                sDialog.dismissWithAnimation();

                                ActivityCompat.requestPermissions((Activity)context,
                                        new String[]{Manifest.permission.RECORD_AUDIO},
                                        AUDIO_PERMISSION);
                            }
                        })
                        .show();


            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        AUDIO_PERMISSION);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }else{
            startAudioActivity();
        }

    }

    /**
     * starts audio activity with current ID passed
     */
    private void startAudioActivity() {
        // We have the permission
        Intent i = new Intent(this,AudioRecordingActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        if(currentId == -1){
            currentId = idGenerator.nextInt(Integer.MAX_VALUE);
        }

        Log.d("D","startAudioActivityDebug with audioFileName = " + audioFilename);

        i.putExtra(Constants.ID_AUDIO_FILE, audioFilename);

        i.putExtra(Constants.ID_KEY,currentId);

        startActivityForResult(i,AUDIO_ACTIVITY_RESULT);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case AUDIO_PERMISSION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // audio recording-related task you need to do.

                    startAudioActivity();

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.

                    displayGenericErrorMessageWithTitle("The audio recording permission is required to add audio to your note 1");
                }
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    /**
     * sets the on click for the drawing button to open the drawing activity
     */
    private void setupDrawingButton(){
        drawingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(context,DrawingActivity.class);
                if(currentId == -1){
                    currentId = idGenerator.nextInt(Integer.MAX_VALUE);
                }

                Log.d("D","drawingDebug with currentId = " + currentId);

                i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                i.putExtra(Constants.ID_KEY,currentId);
                startActivityForResult(i,DRAWING_ACTIVITY_RESULT);
            }
        });
    }

    /**
     * sets the on click for the calendar button to open the calendar activity
     */
    private void setupCalendarButton(){
        calendarButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = inputText.getText().toString();
                if(!text.equals("")){
                    try{
                        launchCalendarFragment();
                    }catch (Exception e){
                        displayGenericErrorMessageWithTitle("Something went wrong");
                    }

                }else{
                    displayGenericErrorMessageWithTitle("Please enter text to save first");
                }
            }
        });
    }

    /**
     * displays generic error message
     * @param title
     */
    private void displayGenericErrorMessageWithTitle(String title){
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
     * displays calendar view which will in turn display the time view which will in turn send the intent to launch the calendar view
     */
    private void launchCalendarFragment(){

        Calendar now = Calendar.getInstance();
        int year = now.get(Calendar.YEAR);
        int month = now.get(Calendar.MONTH); // Note: zero based!
        int day = now.get(Calendar.DAY_OF_MONTH);


        CalendarDatePickerDialogFragment cdp = new CalendarDatePickerDialogFragment()
                .setOnDateSetListener(MainActivity.this)
                .setFirstDayOfWeek(Calendar.SUNDAY)
                .setPreselectedDate(year , month , day)
                .setDoneText("Select Time")
                .setCancelText("Cancel");
        cdp.show(getSupportFragmentManager(), FRAG_TAG_DATE_PICKER);
    }

    @Override
    public void onDateSet(CalendarDatePickerDialogFragment dialog, int year, int month, int day) {
        yearForCalendar = year;
        monthForCalendar = month;
        dayForCalendar = day;

        RadialTimePickerDialogFragment rtpd = new RadialTimePickerDialogFragment()
                .setOnTimeSetListener(MainActivity.this)
                .setDoneText("Okay")
                .setCancelText("Cancel");
        rtpd.show(getSupportFragmentManager(), FRAG_TAG_TIME_PICKER);
    }

    @Override
    public void onTimeSet(RadialTimePickerDialogFragment dialog, int hour, int minute) {
        hourForCalendar = hour;
        minuteForCalendar = minute;

        String text = inputText.getText().toString();

        //Toast.makeText(context, yearForCalendar + " " + monthForCalendar + " " + dayForCalendar + " " + hour + " " + minute,Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(Intent.ACTION_EDIT);
        intent.setType("vnd.android.cursor.item/event");
        intent.putExtra(CalendarContract.Events.TITLE, text);
        GregorianCalendar calDate = new GregorianCalendar(yearForCalendar, monthForCalendar, dayForCalendar, hourForCalendar, minuteForCalendar);
        intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME,
                calDate.getTimeInMillis());
        intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME,
                calDate.getTimeInMillis() + 300000); // begin time + 5 minutes
        startActivity(intent);
    }

    /**
     * Adds logic to close checkbox. If checked, we want the app to close when the user sends a notification
     */
    private void setupCloseCheckbox(){
        closeAppCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                sharedPreferences.edit().putBoolean(CLOSE_APP_KEY,isChecked).apply();
            }
        });

        if(sharedPreferences.getBoolean(CLOSE_APP_KEY,false)){
            closeAppCheckbox.setChecked(true);
        }
    }

    /**
     * sends notification
     */
    private void save(){
        if(currentId == -1){
            //first time sending notification, send it as normal
            sendNotification();
        }else if(!getIntent().hasExtra(TEXT_KEY)){
            //not coming from a clicked notification
            sendNotification();
        }else{
            //coming from a clicked notification
            //we need to generate a new ID so the intent of the notification can have a new payload to associate with itself

            //cancel id
            notificationManager.cancel(currentId);

            //delete old ID
            deleteId(currentId);

            currentId = -1;

            sendNotification();

        }

        resetInformation();
    }

    /**
     * deletes notification
     */
    private void delete(){
        if(currentId != -1){

            new SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
                    .setTitleText("Warning")
                    .setContentText("Are you sure you want to delete?")
                    .setConfirmText("Yes")
                    .setCancelText("No")
                    .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                        @Override
                        public void onClick(SweetAlertDialog sDialog) {
                            notificationManager.cancel(currentId);

                            deleteId(currentId);

                            finish();
                        }
                    })
                    .show();
        }
    }

    /**
     * resets UI / current ID after sending a notification
     */
    private void resetInformation(){
        inputText.setText("");

        currentId = -1;

        persitentCheckbox.setChecked(false);

        image = null;

    }

    /**
     * Delete ID in the db
     * @param ID - notification DB
     */
    private void deleteId(int ID){

        final Notification toDelete = realm.where(Notification.class).equalTo("ID",ID).findFirst();

        // All changes to data must happen in a transaction
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                // remove single match
                toDelete.deleteFromRealm();
            }
        });

    }

    /**
     * grabs text from active edit text, sends notification.
     */
    private void sendNotification() {
        String textToSend = inputText.getText().toString();
        if (textToSend.equals("")) {
            helper.displayErrorMessage("Please enter in a message");
            return;
        }

        int color = ContextCompat.getColor(this, R.color.colorPrimary);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.notif_icon)
                        .setColor(color)
                        .setContentTitle(getResources().getString(R.string.app_name))
                        .setContentText(textToSend);
        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(this, MainActivity.class);
        resultIntent.putExtra(TEXT_KEY,textToSend);
        resultIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        if(persitentCheckbox.isChecked()){
            //make persistent
            mBuilder.setOngoing(true);
            resultIntent.putExtra(PERSISTENCE_KEY,true);

            sharedPreferences.edit().putBoolean(PERSISTENCE_KEY,true).apply();
        }else{
            sharedPreferences.edit().putBoolean(PERSISTENCE_KEY,false).apply();
        }

        int idToPass = -1;
        if(currentId == -1){
            idToPass = idGenerator.nextInt(Integer.MAX_VALUE);
            currentId = idToPass;
            Log.d("D","idDebug generating new ID " + idToPass);
        }else{
            idToPass = currentId;
            Log.d("D","idDebug clicked on a notification " + idToPass);
        }

        resultIntent.putExtra(Constants.ID_KEY,idToPass);

        if(image != null && image.length > 0){
            resultIntent.putExtra(Constants.ID_IMAGE,image);
        }

        if(audioFilename != null && !audioFilename.equals("")){
            resultIntent.putExtra(Constants.ID_AUDIO,audioFilename);

        }

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(MainActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);

        PendingIntent resultPendingIntent = PendingIntent.getActivity(this, idToPass, resultIntent, 0);

        mBuilder.setDeleteIntent(getDeleteIntent(this,idToPass));

        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if(image != null){
            //add big image
            Bitmap bitmap = BitmapFactory.decodeByteArray(image, 0, image.length);
            NotificationCompat.BigPictureStyle s = new NotificationCompat.BigPictureStyle().bigPicture(bitmap);
            s.setSummaryText(textToSend);
            mBuilder.setStyle(s);
        }



        mNotificationManager.notify(idToPass, mBuilder.build());

        saveNotification(textToSend,idToPass,persitentCheckbox.isChecked());

        if(sharedPreferences.getBoolean(CLOSE_APP_KEY,false)){
            finish();
        }
    }

    protected PendingIntent getDeleteIntent(Context context, int ID)
    {
        Intent intent = new Intent(context, OnNotificationDeleted.class);
        intent.setAction("notification_cancelled");
        intent.putExtra(Constants.ID_KEY,ID);
        return PendingIntent.getBroadcast(context, ID+1, intent, PendingIntent.FLAG_CANCEL_CURRENT);
    }


    /**
     * Saves the current notification
     * @param text - body
     * @param ID - randomly generated. Chance for conflict is extremely small.
     * @param persistence - whether or not the user can swipe it away
     */
    private void saveNotification(String text, int ID, boolean persistence){

        RealmResults<Notification> results = realm.where(Notification.class).equalTo("ID",ID).findAll();

        if(results.size() > 0){
            if(realm != null){
                Notification toEdit = realm.where(Notification.class)
                        .equalTo("ID", ID).findFirst();
                realm.beginTransaction();
                toEdit.setText(text);
                toEdit.setPersistent(persistence);
                if(image != null){
                    toEdit.setImage(image);
                }else{
                    toEdit.setImage(new byte[0]);
                }

                Log.d("D","audioFileDebug 1 = " + audioFilename);

                if(audioFilename != null && !audioFilename.equals("")){
                    toEdit.setAudioPath(audioFilename);
                }else{
                    toEdit.setAudioPath("");
                }

                realm.commitTransaction();
            }else{
                realm = Realm.getDefaultInstance();
                saveNotification(text,ID,persistence);
            }
        }else{
            if(realm != null){
                realm.beginTransaction();

                // Create an object
                Notification notification = realm.createObject(Notification.class);
                notification.setText(text);
                notification.setID(ID);
                notification.setPersistent(persistence);
                if(image != null){
                    notification.setImage(image);
                }else{
                    notification.setImage(new byte[0]);
                }

                Log.d("D","audioFileDebug 2 = " + audioFilename);

                if(audioFilename != null && !audioFilename.equals("")){
                    notification.setAudioPath(audioFilename);
                }else{
                    notification.setAudioPath("");
                }

                realm.commitTransaction();
            }else{
                realm = Realm.getDefaultInstance();
                saveNotification(text,ID,persistence);
            }
        }

    }


    /**
     * if the user clicks on a notification, this method will populate the fields with the values
     */
    private void populateFields(){
        String text = getIntent().hasExtra(TEXT_KEY) ? getIntent().getStringExtra(TEXT_KEY) : "";
        Log.d("D","idDebug clickedOnNotification with text = " + text);
        if(!text.equals("")){
            inputText.setText(text);

            boolean persistenceCheck = getIntent().getBooleanExtra(PERSISTENCE_KEY,false);
            if(persistenceCheck){
                persitentCheckbox.setChecked(true);
            }

            Log.d("D","idDebug clickedOnNotification with persistenceCheck = " + persistenceCheck);

            currentId = getIntent().getIntExtra(Constants.ID_KEY,-1);

            image = getIntent().getByteArrayExtra(Constants.ID_IMAGE);

            //set image
            if(image != null && imageViewDisplay != null){
                Bitmap bitmap = BitmapFactory.decodeByteArray(image, 0, image.length);
                imageViewDisplay.setVisibility(View.VISIBLE);
                setImageHeight();
                imageViewDisplay.setImageBitmap(bitmap);
            }

            audioFilename = getIntent().getStringExtra(Constants.ID_AUDIO);

            Log.d("D","idDebug clickedOnNotification with currentId = " + currentId);

            if(audioFilename != null){
                Log.d("D","idDebug clickedOnNotification with audioFilename = " + audioFilename);
            }

            if(image != null ){
                Log.d("D","idDebug clickedOnNotification with image = " + image.length);
            }
        }
    }

    /**
     * sets the imageview that displays the user drawing to be the device height
     */
    private void setImageHeight() {
        if(imageViewDisplay != null){
            try{
                DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
                imageViewDisplay.getLayoutParams().height = displayMetrics.heightPixels;

                //make clicking the image open the drawing activity
                imageViewDisplay.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent i = new Intent(context,DrawingActivity.class);
                        if(currentId == -1){
                            currentId = idGenerator.nextInt(Integer.MAX_VALUE);
                        }

                        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        i.putExtra(Constants.ID_KEY,currentId);
                        startActivityForResult(i,DRAWING_ACTIVITY_RESULT);
                    }
                });


            }catch (Exception e){

            }

        }
    }


    public static class OnBoot extends BroadcastReceiver
    {
        Realm realm;

        public OnBoot(){

        }

        @Override
        public void onReceive(Context context, Intent intent)
        {
            Log.d("D","onBootReceiveDebug");

            // The Realm file will be located in package's "files" directory.
            RealmConfiguration realmConfig = new RealmConfiguration.Builder(context).build();
            Realm.setDefaultConfiguration(realmConfig);
            realm = Realm.getDefaultInstance();

            if(realm != null){
                RealmResults<Notification> allNotifications =
                        realm.where(Notification.class).findAll();

                Log.d("D","onBootReceiveDebug realm size = " + allNotifications.size());

                for(Notification n: allNotifications){
                    Log.d("D","onBootReceiveDebug text = " + n.getText());

                    sendNotification(context,n.getText(),n.getID(),n.isPersistent(), n.getImage(),n.getAudioPath());
                }

            }else{
                Log.d("D","onBootReceiveDebug realm is null");
            }
        }

        /**
         * grabs text from active edit text, sends notification.
         */
        private void sendNotification(Context context, String text, int ID, boolean persistence, byte[] image, String audioFilePath) {

            int color = ContextCompat.getColor(context, R.color.colorPrimary);

            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(context)
                            .setSmallIcon(R.drawable.notif_icon)
                            .setColor(color)
                            .setContentTitle(context.getResources().getString(R.string.app_name))
                            .setContentText(text);
            // Creates an explicit intent for an Activity in your app
            Intent resultIntent = new Intent(context, MainActivity.class);
            resultIntent.putExtra(TEXT_KEY,text);

            if(persistence){
                //make persistent
                mBuilder.setOngoing(true);
                resultIntent.putExtra(PERSISTENCE_KEY,true);
            }

            resultIntent.putExtra(Constants.ID_KEY,ID);

            if(image != null){
                resultIntent.putExtra(Constants.ID_IMAGE,image);
            }

            if(audioFilePath != null && !audioFilePath.equals("")){
                resultIntent.putExtra(Constants.ID_AUDIO,audioFilePath);
            }

            // The stack builder object will contain an artificial back stack for the
            // started Activity.
            // This ensures that navigating backward from the Activity leads out of
            // your application to the Home screen.
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
            // Adds the back stack for the Intent (but not the Intent itself)
            stackBuilder.addParentStack(MainActivity.class);
            // Adds the Intent that starts the Activity to the top of the stack
            stackBuilder.addNextIntent(resultIntent);

            PendingIntent resultPendingIntent = PendingIntent.getActivity(context, ID, resultIntent, 0);

            mBuilder.setDeleteIntent(getDeleteIntent(context,ID));

            mBuilder.setContentIntent(resultPendingIntent);
            NotificationManager mNotificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);


            Log.d("D","onBootReceiveDebug sending notification");
            mNotificationManager.notify(ID, mBuilder.build());

        }

        protected PendingIntent getDeleteIntent(Context context, int ID)
        {
            Intent intent = new Intent(context, OnNotificationDeleted.class);
            intent.setAction("notification_cancelled");
            intent.putExtra(Constants.ID_KEY,ID);
            return PendingIntent.getBroadcast(context, ID+1, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        }


    }


    public static class OnNotificationDeleted extends BroadcastReceiver
    {

        Realm realm;

        public OnNotificationDeleted(){

        }

        @Override
        public void onReceive(Context context, Intent intent)
        {
            int ID = intent.getIntExtra(Constants.ID_KEY,-1);
            if(ID != -1){
                Log.d("D","deleteNotificationDebug with ID = " + ID);
                deleteNotification(context, ID);
            }else{
                Log.d("D","deleteNotificationDebug with ID is bad = " + ID);
                Toast.makeText(context,"Something went wrong", Toast.LENGTH_SHORT).show();
            }

        }

        /**
         * deletes notification
         * @param id
         */
        private void deleteNotification(Context context, int id) {

            // The Realm file will be located in package's "files" directory.
            RealmConfiguration realmConfig = new RealmConfiguration.Builder(context).build();
            Realm.setDefaultConfiguration(realmConfig);
            realm = Realm.getDefaultInstance();

            if(realm != null){
                // obtain the results of a query
                final Notification result = realm.where(Notification.class).equalTo("ID",id).findFirst();

                Log.d("D","deleteNotificationDebug with result = " + (result == null ? "null" : "not null"));

                realm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        result.deleteFromRealm();
                    }
                });

            }else{
                Log.d("D","onBootReceiveDebug realm is null");
            }

        }


    }

    public static class NotifyingDailyService extends Service {

        @Override
        public IBinder onBind(Intent arg0) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public int onStartCommand(Intent pIntent, int flags, int startId) {
            // TODO Auto-generated method stub
            return super.onStartCommand(pIntent, flags, startId);
        }
    }
}
