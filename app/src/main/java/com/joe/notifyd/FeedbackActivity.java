package com.joe.notifyd;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class FeedbackActivity extends AppCompatActivity {

    final String FEEDBACK_EMAIL_ADDRESS = "android.jtr@gmail.com";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);

        final EditText feedbackText = (EditText)findViewById(R.id.feedbackText);

        Button sendButton = (Button)findViewById(R.id.sendButton);
        if(sendButton != null){
            sendButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String textToSend = "";
                    if(feedbackText != null){
                        textToSend = feedbackText.getText().toString();
                    }

                    final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
                    emailIntent.setType("plain/text");
                    emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{FEEDBACK_EMAIL_ADDRESS});
                    emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Feedback - " + getResources().getString(R.string.app_name));
                    emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, textToSend);

                    startActivity(Intent.createChooser(emailIntent, "Send mail"));
                }
            });
        }


    }
}
