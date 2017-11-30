package com.oreshkinsms;

import android.database.Cursor;
import android.os.AsyncTask;
import android.provider.Telephony;

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
import java.util.function.Consumer;

class MakeRequestTask extends AsyncTask<Void, Void, Boolean> {
    private final String spreadsheetId = "1Hng0jVbDq9YPaS9w1cc-B9r1_paz7crP1f5etqaRLAI";
    private com.google.api.services.sheets.v4.Sheets mService = null;
    private Exception mLastError = null;
    private final Cursor cursor;

    MakeRequestTask(GoogleAccountCredential credential, Cursor cursor) {
        this.cursor = cursor;
        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        mService = new com.google.api.services.sheets.v4.Sheets.Builder(
                transport, jsonFactory, credential)
                .setApplicationName("Google Sheets API Android Quickstart")
                .build();
    }


    private void processCursor(Cursor c, Consumer<Payload> payloadConsumer) {
        // Read the sms data and store it in the list
        while (c.moveToNext()) {
            SMSData sms = new SMSData();
            sms.setId(c.getString(c.getColumnIndex(Telephony.Sms.Inbox._ID)));
            sms.setDate(c.getString(c.getColumnIndex(Telephony.Sms.Inbox.DATE)));
            sms.setBody(c.getString(c.getColumnIndex(Telephony.Sms.Inbox.BODY)));
            sms.setNumber(c.getString(c.getColumnIndex(Telephony.Sms.Inbox.ADDRESS)));
            SmsMatcher.parse(sms, payloadConsumer);
        }
        c.close();
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        processCursor(cursor, new Consumer<Payload>() {
            @Override
            public void accept(Payload payload) {
                processPayload(payload);
            }
        });

        return true;
    }

    private boolean processPayload(Payload payload) {
        try {
            boolean lookupEntryFound = lookupEntry(payload);
            if (lookupEntryFound) {
                return true;
            }
            appendEntry(payload);
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

        String freeRowRange = "payments!A" + lastFreeCell + ":D" + lastFreeCell;


        //for the values that you want to input, create a list of object lists
        List<List<Object>> rows = new ArrayList<>();

        //Where each value represents the list of objects that is to be written to a range
        //I simply want to edit a single row, so I use a single list of objects
        List<Object> columns = new ArrayList<>();
        columns.add(payload.getTime());
        columns.add(payload.getSender());
        columns.add(payload.getAmount());
        columns.add(payload.getComment());

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
                .setValueInputOption("RAW")
                .execute();
    }


    @Override
    protected void onPreExecute() {
//        mOutputText.setText("");
//            mProgress.show();
    }

    @Override
    protected void onPostExecute(Boolean output) {
//            mProgress.hide();
//        if (output == null || !output) {
//            mOutputText.setText("No results returned.");
//        } else {
////                output.add(0, "Data retrieved using the Google Sheets API:");
////                mOutputText.setText(TextUtils.join("\n", output));
//            mOutputText.setText("Result ok!");
//        }
    }

    @Override
    protected void onCancelled() {
//            mProgress.hide();
        if (mLastError != null) {

            if (mLastError instanceof UserRecoverableAuthIOException) {
//                startActivityForResult(
//                        ((UserRecoverableAuthIOException) mLastError).getIntent(),
//                        LoginActivity.REQUEST_AUTHORIZATION);
            } else {
//                mOutputText.setText("The following error occurred:\n"
//                        + mLastError.getMessage());
            }
        } else {
//            mOutputText.setText("Request cancelled.");
        }
    }
}
