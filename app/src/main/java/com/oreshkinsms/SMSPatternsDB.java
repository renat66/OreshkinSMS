package com.oreshkinsms;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SMSPatternsDB {
    static final Map<String, List<SmsPattern>> patterns = new HashMap<>();

    static {
        addPattern(patterns, new SmsPattern("(Сбербанк Онлайн\\.)(.*)(перевел\\(а\\) Вам)(\\s*)([0-9]+\\.[0-9]+)(\\s*)(RUB)((\\. Сообщение: \\\"(.*)\\\")*)", "900", 5, 10));
        addPattern(patterns, new SmsPattern("(Popolnenie ot klienta Tinkoff Banka\\.)(.*)(Karta \\*2286\\.)(\\s*)([0-9]+\\.[0-9]+)(\\s*)(RUB)(\\. Otpravitel - (.[^\\.]*))", "Tinkoff", 5, 9));
        addPattern(patterns, new SmsPattern("(Popolnenie\\.)(.*)(Karta \\*2286\\.)( Summa *)([0-9]+\\.[0-9]+)(\\s*)(RUB)", "Tinkoff", 5, null));

        //sber (Сбербанк Онлайн\.)(.*)(перевел\(а\) Вам)(\s*)([0-9]+\.[0-9]+)(\s*)(RUB)((\. Сообщение:.*)*)  S=5 C=10
        //tinkoff to tinkof (Popolnenie ot klienta Tinkoff Banka\.)(.*)(Karta \*2286\.)(\s*)([0-9]+\.[0-9]+)(\s*)(RUB)(\. Otpravitel - (.[^\.]*)) S=5 C=9
        //tinkoff other (Popolnenie\.)(.*)(Karta \*2286\.)( Summa *)([0-9]+\.[0-9]+)(\s*)(RUB)  S=5
//        Pattern sberPattern = Pattern.compile("(Сбербанк Онлайн\\.)(.*)(перевел\\(а\\) Вам)(\\s*)([0-9]+\\.[0-9]+)(\\s*)(RUB)((\\. Сообщение:.*)*)");

    }


    private static void addPattern(Map<String, List<SmsPattern>> patterns, SmsPattern smsPattern) {
        List<SmsPattern> smsPatterns = patterns.get(smsPattern.getSender());
        if (smsPatterns == null) {
            smsPatterns = new ArrayList<>();
            patterns.put(smsPattern.getSender(), smsPatterns);
        }
        smsPatterns.add(smsPattern);
    }

}
