package com.oreshkinsms;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.icu.text.SimpleDateFormat;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.provider.Telephony;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.sheets.v4.SheetsScopes;

import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.function.Function;
import java.util.stream.Collectors;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class LoginActivity extends AppCompatActivity {
    GoogleAccountCredential mCredential;
    ProgressDialog mProgress;
    private MakeRequestTask makeRequestTask;
    Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize credentials and service object.
        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Collections.singletonList(SheetsScopes.SPREADSHEETS))
                .setBackOff(new ExponentialBackOff());

        requestSMSReceive();

        mProgress = new ProgressDialog(this);
        mProgress.setMessage("Exporting payment SMS to google sheet");

        button = findViewById(R.id.buttonStartAction);
        button.setOnClickListener(new View.OnClickListener() {


            public void onClick(View v) {
                button.setEnabled(false);
                // Code here executes on main thread after user presses button
                Cursor cursor = makeSMSRequest();
                makeRequestTask = new MakeRequestTask(mCredential, cursor, mProgress, button, LoginActivity.this);
                makeRequestTask.execute();
            }
        });
    }


    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;
    static final int REQUEST_PERMISSION_RECEIVE_SMS = 1010;
    static final int REQUEST_PERMISSION_READ_SMS = 1011;
    private static final String PREF_ACCOUNT_NAME = "accountName";

    @AfterPermissionGranted(REQUEST_PERMISSION_RECEIVE_SMS)
    private void requestSMSReceive() {
        if (EasyPermissions.hasPermissions(this, Manifest.permission.RECEIVE_SMS)) {
            Toast.makeText(this, "SMS receive granted", Toast.LENGTH_SHORT).show();
            requestSMSRead();
        } else {
            // Request the GET_ACCOUNTS permission via a user dialog
            EasyPermissions.requestPermissions(
                    this,
                    "This app needs to access your SMS",
                    REQUEST_PERMISSION_RECEIVE_SMS,
                    Manifest.permission.RECEIVE_SMS);
        }
    }

    @AfterPermissionGranted(REQUEST_PERMISSION_READ_SMS)
    private void requestSMSRead() {
        if (EasyPermissions.hasPermissions(this, Manifest.permission.READ_SMS)) {
            Toast.makeText(this, "SMS read granted", Toast.LENGTH_SHORT).show();
            chooseAccount();
        } else {
            // Request the GET_ACCOUNTS permission via a user dialog
            EasyPermissions.requestPermissions(
                    this,
                    "This app needs to access your SMS",
                    REQUEST_PERMISSION_READ_SMS,
                    Manifest.permission.READ_SMS);
        }
    }

    /**
     * Attempts to set the account used with the API credentials. If an account
     * name was previously saved it will use that one; otherwise an account
     * picker dialog will be shown to the user. Note that the setting the
     * account to use with the credentials object requires the app to have the
     * GET_ACCOUNTS permission, which is requested here if it is not already
     * present. The AfterPermissionGranted annotation indicates that this
     * function will be rerun automatically whenever the GET_ACCOUNTS permission
     * is granted.
     */
    @AfterPermissionGranted(REQUEST_PERMISSION_GET_ACCOUNTS)
    private void chooseAccount() {
        if (EasyPermissions.hasPermissions(this, Manifest.permission.GET_ACCOUNTS)) {
            String accountName = getPreferences(Context.MODE_PRIVATE)
                    .getString(PREF_ACCOUNT_NAME, null);
            if (accountName != null) {
                mCredential.setSelectedAccountName(accountName);
//                getResultsFromApi();
            } else {

                // Start a dialog from which the user can choose an account
                startActivityForResult(
                        mCredential.newChooseAccountIntent(),
                        REQUEST_ACCOUNT_PICKER);
            }
        } else {
            // Request the GET_ACCOUNTS permission via a user dialog
            EasyPermissions.requestPermissions(
                    this,
                    "This app needs to access your Google account (via Contacts).",
                    REQUEST_PERMISSION_GET_ACCOUNTS,
                    Manifest.permission.GET_ACCOUNTS);
        }
    }

    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_ACCOUNT_PICKER: {
                if (resultCode == RESULT_OK && data != null &&
                        data.getExtras() != null) {
                    String accountName =
                            data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        SharedPreferences settings =
                                getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.apply();
                        mCredential.setSelectedAccountName(accountName);
//                        getResultsFromApi();
                    }
                }
                break;
            }
            case REQUEST_AUTHORIZATION:
                if (resultCode == RESULT_OK) {
                    mProgress.hide();
                    button.setEnabled(true);
//                    getResultsFromApi();
                }
                break;
        }
    }

    /**
     * Respond to requests for permissions at runtime for API 23 and above.
     *
     * @param requestCode  The request code passed in
     *                     requestPermissions(android.app.Activity, String, int, String[])
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *                     which is either PERMISSION_GRANTED or PERMISSION_DENIED. Never null.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    private boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    private Cursor makeSMSRequest() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        String selectedDate = 2017 + "-" + 11 + "-" + 20;
        Date dateStart = null;
        try {
            dateStart = formatter.parse(selectedDate + "T00:00:00");
        } catch (ParseException e) {
            e.printStackTrace();
        }

        String sendersFilter = SMSPatternsDB.patterns.keySet().stream().map(new Function<String, String>() {
            @Override
            public String apply(String s) {
                return "address=\"" + s + "\"";
            }
        }).collect(Collectors.joining(" or ", "(", ")"));

// Now create the filter and query the messages.
        String filter = "date>=" + dateStart.getTime() + " and " + sendersFilter;


        CursorLoader cl = new CursorLoader(getApplicationContext());
//        getContentResolver().query(SMS_INBOX, null, filter, null, null);

        cl.setSelection(filter);
        cl.setUri(Telephony.Sms.Inbox.CONTENT_URI);
        cl.setProjection(new String[]{
                Telephony.Sms.Inbox._ID,
                Telephony.Sms.Inbox.BODY,
                Telephony.Sms.Inbox.DATE,
                Telephony.Sms.Inbox.ADDRESS,
        });
        cl.setSortOrder("date DESC"); // to upload fresh sms first
        return cl.loadInBackground();
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////

}
