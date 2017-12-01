package com.oreshkinsms;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.database.Cursor;
import android.os.AsyncTask;
import android.provider.Telephony;
import android.widget.Button;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

class MakeRequestTask extends AsyncTask<Void, Void, Boolean> {
    private final String spreadsheetId = "1K6IneCuXl-IoxcxbJFXlTe2uImg_NOQ5xs_v2hUf_ck";
    private com.google.api.services.sheets.v4.Sheets mService = null;
    private Exception mLastError = null;
    private final Cursor cursor;
    private final ProgressDialog mProgress;
    final Button button;
    private final Activity loginActivity;

    int uploadedSmsCounter = 0;

    MakeRequestTask(GoogleAccountCredential credential, Cursor cursor, ProgressDialog mProgress, Button button, LoginActivity loginActivity) {
        this.cursor = cursor;
        this.mProgress = mProgress;
        this.button = button;
        this.loginActivity = loginActivity;
        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        mService = new com.google.api.services.sheets.v4.Sheets.Builder(
                transport, jsonFactory, credential)
                .setApplicationName("Google Sheets API Android Quickstart")
                .build();
    }


    private void processCursor(Cursor c) {
        // Read the sms data and store it in the list
        try {
            while (c.moveToNext()) {
                SMSData sms = new SMSData();
                sms.setId(c.getString(c.getColumnIndex(Telephony.Sms.Inbox._ID)));
                sms.setDate(c.getString(c.getColumnIndex(Telephony.Sms.Inbox.DATE)));
                sms.setBody(c.getString(c.getColumnIndex(Telephony.Sms.Inbox.BODY)));
                sms.setNumber(c.getString(c.getColumnIndex(Telephony.Sms.Inbox.ADDRESS)));

                Optional<Payload> payload = SmsMatcher.parse(sms);
                if (payload.isPresent()) {
                    if (!processPayload(payload.get())) {
                        break;
                    }
                }

            }
        } finally {
            c.close();
        }
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        processCursor(cursor);

        return true;
    }

    private boolean processPayload(Payload payload) {
        try {
            boolean lookupEntryFound = lookupEntry(payload);
            if (lookupEntryFound) {
                return true;
            }
            appendEntry(payload);
            uploadedSmsCounter++;
            return true;
        } catch (Exception e) {
            mLastError = e;
            cancel(true);
            return false;
        }
    }


    private boolean lookupEntry(Payload payload) throws IOException {
        String rangeLookupKeys = "payments!J6:L6";
        String rangeWithLookupResult = "payments!J7:J7";

        //for the values that you want to input, create a list of object lists
        List<List<Object>> rows = new ArrayList<>();

        //Where each value represents the list of objects that is to be written to a range
        //I simply want to edit a single row, so I use a single list of objects
        List<Object> columns = new ArrayList<>();
        columns.add(payload.getSender());
        columns.add(payload.getAmount());
        columns.add(payload.getTime());
//            data1.add(comments);

        //There are obviously more dynamic ways to do these, but you get the picture
        rows.add(columns);
//            values.add(Collections.<Object>singletonList(number));

        //Create the valuerange object and set its fields
        ValueRange valueRange = new ValueRange();
        valueRange.setMajorDimension("ROWS");
        valueRange.setRange(rangeLookupKeys);
        valueRange.setValues(rows);

        UpdateValuesResponse raw = this.mService.spreadsheets().values()
                .update(spreadsheetId, rangeLookupKeys, valueRange)
                .setValueInputOption("RAW")
                .execute();
        raw.getSpreadsheetId();

        ValueRange lookupResultValues = this.mService.spreadsheets().values()
                .get(spreadsheetId, rangeWithLookupResult)
                .execute();

        Object lookupResultCellValue = lookupResultValues.getValues().iterator().next().iterator().next();

        if (lookupResultCellValue == null || "not found".equals(lookupResultCellValue.toString())) {
            return false;
        }
        return true;
    }

    private void appendEntry(Payload payload) throws IOException {
        ValueRange lookupResultValues = this.mService.spreadsheets().values()
                .get(spreadsheetId, "payments!J5:J5")
                .execute();

        Object lastFreeCellStr = lookupResultValues.getValues().iterator().next().iterator().next();

        int lastFreeCell = Integer.parseInt(lastFreeCellStr.toString());

        String freeRowRange = "payments!A" + lastFreeCell + ":H" + lastFreeCell;


        //for the values that you want to input, create a list of object lists
        List<List<Object>> rows = new ArrayList<>();

        //Where each value represents the list of objects that is to be written to a range
        //I simply want to edit a single row, so I use a single list of objects
        List<Object> columns = new ArrayList<>();
        columns.add(payload.getTime());
        columns.add(payload.getSender());
        columns.add(payload.getAmount());
        columns.add(payload.getComment() != null ? payload.getComment() : " ");

        columns.add("=IFERROR(QUERY({'Сухофрукты/Орехи'!B:B,'Сухофрукты/Орехи'!CJ:CJ}, (\"select Col1 where Col2=\"&C" + lastFreeCell + "&\" limit 1\"),0),\"?\")");
        columns.add("=IFERROR(QUERY({'Сухофрукты/Орехи'!B:B,'Сухофрукты/Орехи'!CJ:CJ}, (\"select count(Col1) where Col2=\"&C" + lastFreeCell + "&\" label count(Col1) ''\"),1),)");
        columns.add(" ");
        columns.add("=IFERROR(QUERY({'Сухофрукты/Орехи'!B:B,'Сухофрукты/Орехи'!CJ:CJ}, (\"select Col1 where Col2=\"&C" + lastFreeCell + "&\" offset 1\"),0),\"нет\")");

        //There are obviously more dynamic ways to do these, but you get the picture
        rows.add(columns);
//            values.add(Collections.<Object>singletonList(number));

        //Create the valuerange object and set its fields
        ValueRange valueRange = new ValueRange();
        valueRange.setMajorDimension("ROWS");
        valueRange.setRange(freeRowRange);
        valueRange.setValues(rows);

        UpdateValuesResponse raw = this.mService.spreadsheets().values()
                .update(spreadsheetId, freeRowRange, valueRange)
                .setValueInputOption("USER_ENTERED")
                .execute();
    }


    @Override
    protected void onPreExecute() {
        mProgress.show();
    }

    @Override
    protected void onPostExecute(Boolean output) {
        mProgress.hide();
        button.setEnabled(true);
        AlertDialog.Builder alert = new AlertDialog.Builder(loginActivity);
        String title = "Comleted " + (mLastError != null ? "with fail" : "sucessfully");
        alert.setTitle(title);
        alert.setMessage("Uploaded " + uploadedSmsCounter + " new messages. Errors: " + mLastError);
        alert.setPositiveButton("OK", null);
        alert.show();
    }

    @Override
    protected void onCancelled() {
        mProgress.hide();
        button.setEnabled(true);
        if (mLastError != null) {

            if (mLastError instanceof UserRecoverableAuthIOException) {
                loginActivity.startActivityForResult(
                        ((UserRecoverableAuthIOException) mLastError).getIntent(),
                        LoginActivity.REQUEST_AUTHORIZATION);
            } else {
//                mOutputText.setText("The following error occurred:\n"
//                        + mLastError.getMessage());
            }
        } else {
//            mOutputText.setText("Request cancelled.");
        }
    }
}
