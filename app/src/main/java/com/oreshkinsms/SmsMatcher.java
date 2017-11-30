package com.oreshkinsms;

import android.util.Log;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Matcher;


public class SmsMatcher {

    public static void parse(SMSData singleSms, Consumer<Payload> payloadConsumer) {
        List<SmsPattern> patterns = SMSPatternsDB.patterns.get(singleSms.getSender());
        if (patterns == null) {
            return;
        }
        for (SmsPattern smsPattern : patterns) {
            Matcher matcher = smsPattern.getPattern().matcher(singleSms.getBody());
            if (matcher.matches()) {
                final String summStr = matcher.group(smsPattern.getSummIndex());
                String commentStr = "";
                Optional<Integer> commentIndex = smsPattern.getCommentIndex();
                if (commentIndex.isPresent()) {
                    commentStr = matcher.group(commentIndex.get());
                }

                String text = "Input payment:" + singleSms.getSender() + " Summ=" + summStr + " " + commentStr;
                Log.i("tag", text);
                Payload payload = new Payload(singleSms.getSender(), summStr, singleSms.getDate(), commentStr);
                payloadConsumer.accept(payload);
                return;
            }
        }
    }
}
