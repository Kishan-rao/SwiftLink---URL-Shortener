package com.kishanrao.shortener.infra.utils;

import lombok.experimental.UtilityClass;

import java.math.BigInteger;

@UtilityClass
public class Base62Util {

    private static final String CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int BASE = 62;
    private static final int PAD_LENGTH = 8;

    public static String encode(BigInteger number) {
        if (number.signum() == 0) return "0".repeat(PAD_LENGTH);

        var sb = new StringBuilder();
        var base = BigInteger.valueOf(BASE);
        var n = number;

        while (n.signum() > 0) {
            BigInteger[] divRem = n.divideAndRemainder(base);
            sb.append(CHARS.charAt(divRem[1].intValue()));
            n = divRem[0];
        }

        String raw = sb.reverse().toString();
        // Pad to fixed length for aesthetic consistency
        return raw.length() >= PAD_LENGTH ? raw.substring(0, PAD_LENGTH) : "0".repeat(PAD_LENGTH - raw.length()) + raw;
    }
}
