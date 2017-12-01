package com.oreshkinsms;


import java.util.Date;

public class Payload {
    private final String sender;
    private final Long amount;
    private final String time;
    private final String comment;

    public Payload(String sender, String amount, String time, String comment) {
        this.sender = sender;
        this.amount = Double.valueOf(amount).longValue();

        this.time = new Date(Long.parseLong(time)).toString();
        this.comment = comment;
    }


    public String getSender() {
        return sender;
    }

    public Long getAmount() {
        return amount;
    }

    public String getTime() {
        return time;
    }

    public String getComment() {
        return comment;
    }
}
