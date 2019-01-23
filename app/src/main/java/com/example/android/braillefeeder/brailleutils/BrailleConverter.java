package com.example.android.braillefeeder.brailleutils;

import java.util.ArrayList;
import java.util.List;

public class BrailleConverter {

    BrailleConverter() {

    }

    public List<String> convertFromWords(String... words) {
        List<String> braille = new ArrayList<>();
        for( int i = 0; i < words.length; i++) {
            braille.addAll(convertFromWord(words[i]));
            if( i < words.length - 1) {
                braille.add(Braille.SPACE.getValue());
            }
        }
        return braille;
    }

    public List<String> convertFromWord(String text) {
        List<String> braille = new ArrayList<>();
        for( int i = 0; i < text.length(); i++) {
            braille.add(Braille.fromKey(Character.toLowerCase(text.charAt(i))).getValue());
        }
        return braille;
    }
}
