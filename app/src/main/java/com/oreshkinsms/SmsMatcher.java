package com.oreshkinsms;

import android.util.Log;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class SmsMatcher {

    public static Optional<Payload> parse(SMSData singleSms) {
        List<SmsPattern> patterns = SMSPatternsDB.patterns.get(singleSms.getSender());
        if (patterns == null) {
            return Optional.empty();
        }
        String body = singleSms.getBody();
        for (SmsPattern smsPattern : patterns) {
            Pattern pattern = smsPattern.getPattern();
            Matcher matcher = pattern.matcher(body);
            if (matcher.matches()) {
                final String summStr = matcher.group(smsPattern.getSummIndex());
                String commentStr = "";
                Optional<Integer> commentIndex = smsPattern.getCommentIndex();
                if (commentIndex.isPresent()) {
                    commentStr = matcher.group(commentIndex.get());
                }

                String text = "Input payment:" + singleSms.getSender() + " Summ=" + summStr + " " + commentStr;
                Log.i("tag", text);
                return Optional.of(new Payload(singleSms.getSender(), summStr, singleSms.getDate(), commentStr));
            } else {
                String text = "Message rejected: " + body + " for pattern: " + pattern.toString();
                Log.i("tag", text);
            }
        }
        String text = "Message rejected: " + body;
        Log.i("tag", text);
        return Optional.empty();
    }
}
