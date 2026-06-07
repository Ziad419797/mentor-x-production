package com.educore.student;

import java.security.SecureRandom;

public class StudentCodeGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    /** Returns a 6-digit numeric code, e.g. "482031" */
    public static String generate() {
        int number = 100000 + RANDOM.nextInt(900000);
        return String.valueOf(number);
    }
}
