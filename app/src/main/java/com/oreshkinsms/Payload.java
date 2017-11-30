package com.oreshkinsms;


import java.util.Date;

public class Payload {
    private final String sender;
    private final String amount;
    private final String time;
    private final String comment;

    public Payload(String sender, String amount, String time, String comment) {
        this.sender = sender;
        this.amount = amount;

        this.time = new Date(Long.parseLong(time)).toString();
        this.comment = comment;
    }


    public String getSender() {
        return sender;
    }

    public String getAmount() {
        return amount;
    }

    public String getTime() {
        return time;
    }

    public String getComment() {
        return comment;
    }
}
