package com.oreshkinsms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;


public class IncomingSmsReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

          /* Retrieve the sms message chunks from the intent */
        SmsMessage[] rawSmsChunks;
        try {
            rawSmsChunks = Telephony.Sms.Intents.getMessagesFromIntent(intent);
        } catch (NullPointerException ignored) {
            return;
        }

        /* Gather all sms chunks for each sender separately */
        for (SmsMessage rawSmsChunk : rawSmsChunks) {
            if (rawSmsChunk != null) {
                String senderNum = rawSmsChunk.getDisplayOriginatingAddress();
                String message = rawSmsChunk.getDisplayMessageBody();

                if (!SMSPatternsDB.patterns.containsKey(senderNum)) {
                    //process only payment-related messages
                    return;
                }

//                SharedPreferences prefs = context.getSharedPreferences("myPrefs",
//                        Context.MODE_PRIVATE);

                Log.i("SmsReceiver", "senderNum: " + senderNum + "; message: " + message);

                // Show alert
                int duration = Toast.LENGTH_LONG;
                Toast toast = Toast.makeText(context, "senderNum: " + senderNum + ", message: " + message, duration);
                toast.show();
            }
        }
    }
}
