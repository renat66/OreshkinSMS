package com.oreshkinsms;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SMSPatternsDB {
    static final Map<String, List<SmsPattern>> patterns = new HashMap<>();

    static {
        addPattern(new SmsPattern("(Сбербанк Онлайн\\.)(.*)(перевел\\(а\\) Вам)(\\s*)([0-9]+\\.[0-9]+)(\\s*)(RUB)((\\. Сообщение: \\\"(.*)\\\")*)", "900", 5, 10));
        addPattern(new SmsPattern("(Сбербанк Онлайн\\.)(.*)(перевел\\(а\\) Вам)(\\s*)([0-9]+\\.[0-9]+)(\\s*)(.*)", "900", 5, null));
        addPattern(new SmsPattern("(Popolnenie ot klienta Tinkoff Banka\\.)(.*)(Karta \\*2286\\.)(\\s*)([0-9]+\\.[0-9]+)(\\s*)(RUB)(\\. Otpravitel - (.[^\\.]*))", "Tinkoff", 5, 9));
        addPattern(new SmsPattern("(Popolnenie\\.)(.*)(Karta \\*2286\\.)( Summa *)([0-9]+\\.[0-9]+)(\\s*)(RUB)", "Tinkoff", 5, null));
        addPattern(new SmsPattern("(Popolnenie ot klienta Tinkoff Banka\\.)(.*)(Karta \\*2286\\.\\s*Summa )([0-9]+\\.[0-9]+)(\\s*)(RUB)(\\.\\s*Otpravitel\\s*-(.*))", "Tinkoff", 4, 8));
        addPattern(new SmsPattern("(Balans vashey karty \\*7133 popolnilsya )(\\d\\d/\\d\\d/\\d\\d\\d\\d)( na )([0-9]+\\.[0-9]+)(.*)", "Raiffeisen", 4, null));

        //sber (Сбербанк Онлайн\.)(.*)(перевел\(а\) Вам)(\s*)([0-9]+\.[0-9]+)(\s*)(RUB)((\. Сообщение:.*)*)  S=5 C=10
        //tinkoff to tinkof (Popolnenie ot klienta Tinkoff Banka\.)(.*)(Karta \*2286\.)(\s*)([0-9]+\.[0-9]+)(\s*)(RUB)(\. Otpravitel - (.[^\.]*)) S=5 C=9
        //tinkoff other (Popolnenie\.)(.*)(Karta \*2286\.)( Summa *)([0-9]+\.[0-9]+)(\s*)(RUB)  S=5
//(Popolnenie ot klienta Tinkoff Banka\.)(.*)(Karta \*2286\.\s*Summa )([0-9]+\.[0-9]+)(\s*)(RUB)(\.\s*Otpravitel\s*-\s*(.[^\.]*)) //!!! modified
//        Pattern sberPattern = Pattern.compile("(Сбербанк Онлайн\\.)(.*)(перевел\\(а\\) Вам)(\\s*)([0-9]+\\.[0-9]+)(\\s*)(RUB)((\\. Сообщение:.*)*)");
        //Rainf (Balans vashey karty \*7133 popolnilsya )(\d\d/\d\d/\d\d\d\d)( na )([0-9]+\.[0-9]+)(.*)
    }


    private static void addPattern(SmsPattern smsPattern) {
        List<SmsPattern> smsPatterns = patterns.get(smsPattern.getSender());
        if (smsPatterns == null) {
            smsPatterns = new ArrayList<>();
            patterns.put(smsPattern.getSender(), smsPatterns);
        }
        smsPatterns.add(smsPattern);
    }

}
