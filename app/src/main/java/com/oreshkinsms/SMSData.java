package com.oreshkinsms;

public class SMSData {
    String id;

    public void setId(String id) {
        this.id = id;
    }

    // Number from witch the sms was send
    private String number;
    // SMS text body
    private String body;
    private String date;

    public String getSender() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getId() {
        return id;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}