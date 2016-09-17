package com.joe.notifyd;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.joe.notifyd.RealmObjects.Notification;
import com.joe.notifyd.Util.Constants;
import com.joe.notifyd.Util.Helper;

import java.util.Random;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;

public class MainActivity extends AppCompatActivity {

    /**
     * FINAL
     */
    private static final String TEXT_KEY = "text";
    private static final String PERSISTENCE_KEY = "persistence";
    private static final String CLOSE_APP_KEY = "closeApp";
    private static final int DRAWING_ACTIVITY_RESULT = 109;

    /**
     * UI
     */
    @BindView(R.id.saveButton)
    Button saveButton;

    @BindView(R.id.cancelButton)
    Button deleteButton;

    @BindView(R.id.notificationInput)
    com.rengwuxian.materialedittext.MaterialEditText inputText;

    @BindView(R.id.drawingButton)
    Button drawingButton;

    @BindView(R.id.persitentCheckbox)
    CheckBox persitentCheckbox;

    @BindView(R.id.closeAppCheckbox)
    CheckBox closeAppCheckbox;

    /**
     * NON UI
     */

    private Helper helper;

    private SharedPreferences sharedPreferences;

    private NotificationManager notificationManager;

    private Random idGenerator;

    private Realm realm;

    private Context context;


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

        setupSaveButton();

        populateFields();

        setupCloseCheckbox();

        setupDrawingButton();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        Log.d("D","onActivityResultDebug request and result code = " + requestCode + " " + resultCode);

        if (requestCode == DRAWING_ACTIVITY_RESULT) {
            if(resultCode == Activity.RESULT_OK){
                int returnedId = data.getIntExtra(Constants.ID_ACTIVITY_RESULT_GOOD_KEY, -1);

                Log.d("D","onActivityResultDebug with returnedId = " + returnedId);
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                //Write your code if there's no result
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
     * Setup save button logic (sends notification)
     */
    private void setupSaveButton() {
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(currentId == -1){
                    //first time sending notification, send it as normal
                    sendNotification();
                }else{
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
        });
    }

    /**
     * resets UI / current ID after sending a notification
     */
    private void resetInformation(){
        inputText.setText("");

        currentId = -1;

        persitentCheckbox.setChecked(false);
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

        if(persitentCheckbox.isChecked()){
            //make persistent
            mBuilder.setOngoing(true);
            resultIntent.putExtra(PERSISTENCE_KEY,true);
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

            Log.d("D","idDebug clickedOnNotification with currentId = " + currentId);

            //if we're loading a notification, show the delete button
            deleteButton.setVisibility(View.VISIBLE);

            setupDeleteButton();
        }
    }

    /**
     * handles logic for delete button
     */
    private void setupDeleteButton(){
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(currentId != -1){

                    notificationManager.cancel(currentId);

                    deleteId(currentId);

                    finish();
                }
            }
        });


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

                    sendNotification(context,n.getText(),n.getID(),n.isPersistent());
                }

            }else{
                Log.d("D","onBootReceiveDebug realm is null");
            }
        }

        /**
         * grabs text from active edit text, sends notification.
         */
        private void sendNotification(Context context, String text, int ID, boolean persistence) {

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

    public class NotifyingDailyService extends Service {

        @Override
        public IBinder onBind(Intent arg0) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public int onStartCommand(Intent pIntent, int flags, int startId) {
            // TODO Auto-generated method stub
            Toast.makeText(this, "NotifyingDailyService", Toast.LENGTH_LONG).show();
            return super.onStartCommand(pIntent, flags, startId);
        }
    }
}
