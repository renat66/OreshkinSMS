package com.oreshkinsms;


import java.util.Optional;
import java.util.regex.Pattern;

public class SmsPattern {
    private final Pattern pattern;
    private final String sender;
    private final int summIndex;
    private final Integer commentIndex;

    public SmsPattern(String pattern, String sender, int summIndex, Integer commentIndex) {
        this.sender = sender;
        this.summIndex = summIndex;
        this.commentIndex = commentIndex;
        this.pattern = Pattern.compile(pattern);
    }

    public Pattern getPattern() {
        return pattern;
    }

    public String getSender() {
        return sender;
    }

    public int getSummIndex() {
        return summIndex;
    }

    public Optional<Integer> getCommentIndex() {
        return Optional.ofNullable(commentIndex);
    }
}
