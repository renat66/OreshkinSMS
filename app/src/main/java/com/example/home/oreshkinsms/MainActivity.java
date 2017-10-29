package com.example.home.oreshkinsms;

import android.Manifest;
import android.app.ListActivity;
import android.content.CursorLoader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.Telephony;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Matcher;

public class MainActivity extends AppCompatActivity {

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Check whether your app has access to the READ permission//

        if (checkPermission()) {

//If your app has access to the device’s storage, then print the following message to Android Studio’s Logcat//

            Log.e("permission", "Permission already granted.");
        } else {

//If your app doesn’t have permission to access external storage, then call requestPermission//

            requestPermission();
        }
        CursorLoader cl = new CursorLoader(getApplicationContext());


        cl.setUri(Telephony.Sms.Inbox.CONTENT_URI);
        cl.setProjection(new String[]{
                Telephony.Sms.Inbox._ID,
                Telephony.Sms.Inbox.BODY,
                Telephony.Sms.Inbox.DATE,
                Telephony.Sms.Inbox.ADDRESS,
        });
        cl.setSortOrder("date DESC");
        final Cursor c = cl.loadInBackground();

        new Thread(new Runnable() {
            @Override
            public void run() {
//                List<SMSData> smsList = new ArrayList<>();


                TreeMap<String, List<SMSData>> smss = new TreeMap<>();
                // Read the sms data and store it in the list
                if (c.moveToFirst()) {
                    for (int i = 0; i < c.getCount(); i++) {
                        SMSData sms = new SMSData();
                        sms.setId(c.getString(c.getColumnIndex(Telephony.Sms.Inbox._ID)));
                        sms.setDate(c.getString(c.getColumnIndex(Telephony.Sms.Inbox.DATE)));
                        sms.setBody(c.getString(c.getColumnIndex(Telephony.Sms.Inbox.BODY)));
                        sms.setNumber(c.getString(c.getColumnIndex(Telephony.Sms.Inbox.ADDRESS)));
//                        smsList.add(sms);
                        List<SMSData> smsData = smss.get(sms.getNumber());
                        if (smsData == null) {
                            smsData = new ArrayList<>();
                            smss.put(sms.getNumber(), smsData);
                        }
                        smsData.add(sms);

                        c.moveToNext();
                    }
                }
                c.close();


                Map<String, List<SmsPattern>> patterns = new HashMap<>();
                addPattern(patterns, new SmsPattern("(Сбербанк Онлайн\\.)(.*)(перевел\\(а\\) Вам)(\\s*)([0-9]+\\.[0-9]+)(\\s*)(RUB)((\\. Сообщение: \\\"(.*)\\\")*)", "900", 5, 10));
                addPattern(patterns, new SmsPattern("(Popolnenie ot klienta Tinkoff Banka\\.)(.*)(Karta \\*2286\\.)(\\s*)([0-9]+\\.[0-9]+)(\\s*)(RUB)(\\. Otpravitel - (.[^\\.]*))", "Tinkoff", 5, 9));
                addPattern(patterns, new SmsPattern("(Popolnenie\\.)(.*)(Karta \\*2286\\.)( Summa *)([0-9]+\\.[0-9]+)(\\s*)(RUB)", "Tinkoff", 5, null));

                //sber (Сбербанк Онлайн\.)(.*)(перевел\(а\) Вам)(\s*)([0-9]+\.[0-9]+)(\s*)(RUB)((\. Сообщение:.*)*)  S=5 C=10
                //tinkoff to tinkof (Popolnenie ot klienta Tinkoff Banka\.)(.*)(Karta \*2286\.)(\s*)([0-9]+\.[0-9]+)(\s*)(RUB)(\. Otpravitel - (.[^\.]*)) S=5 C=9
                //tinkoff other (Popolnenie\.)(.*)(Karta \*2286\.)( Summa *)([0-9]+\.[0-9]+)(\s*)(RUB)  S=5
//        Pattern sberPattern = Pattern.compile("(Сбербанк Онлайн\\.)(.*)(перевел\\(а\\) Вам)(\\s*)([0-9]+\\.[0-9]+)(\\s*)(RUB)((\\. Сообщение:.*)*)");

                for (Map.Entry<String, List<SMSData>> entry : smss.entrySet()) {
                    final String sender = entry.getKey();
                    List<SmsPattern> patterns1 = patterns.get(sender);
                    if (patterns1 != null) {
                        List<SMSData> messagesFromThisSender = entry.getValue();

                        for (SMSData singleSms : messagesFromThisSender) {
                            for (SmsPattern smsPattern : patterns1) {
                                Matcher matcher = smsPattern.getPattern().matcher(singleSms.getBody());
                                if (matcher.matches()) {
                                    final String summStr = matcher.group(smsPattern.getSummIndex());
                                    String commentStr = null;
                                    Optional<Integer> commentIndex = smsPattern.getCommentIndex();
                                    if (commentIndex.isPresent()) {
                                        commentStr = matcher.group(commentIndex.get());
                                    }

                                    final String finalCommentStr = commentStr;
                                    final String text = "Input payment:" + sender + " Summ=" + summStr + (finalCommentStr != null ? " " + finalCommentStr : "");
                                    Log.i("tag", text);
                                    MainActivity.this.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(MainActivity.this, text, Toast.LENGTH_LONG).show();
                                        }
                                    });

                                }
                            }

                        }

                    }
                }

            }
        }).start();
//        Log.e("permission", "thread started");

        // Set smsList in the ListAdapter
//        setListAdapter(new ListAdapter(this, smsList));

    }

    private void addPattern(Map<String, List<SmsPattern>> patterns, SmsPattern smsPattern) {
        List<SmsPattern> smsPatterns = patterns.get(smsPattern.getSender());
        if (smsPatterns == null) {
            smsPatterns = new ArrayList<>();
            patterns.put(smsPattern.getSender(), smsPatterns);
        }
        smsPatterns.add(smsPattern);
    }


    final static int PERMISSION_REQUEST_CODE = 1;

    private void requestPermission() {

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_SMS}, PERMISSION_REQUEST_CODE);

    }

    private boolean checkPermission() {

//Check for READ_EXTERNAL_STORAGE access, using ContextCompat.checkSelfPermission()//

        int result = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_SMS);

//If the app does have this permission, then return true//

        if (result == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {

//If the app doesn’t have this permission, then return false//

            return false;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    Toast.makeText(MainActivity.this,
                            "Permission accepted", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MainActivity.this,
                            "Permission denied", Toast.LENGTH_LONG).show();

                }
                break;
        }
    }
}
