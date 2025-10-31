package br.edu.ufersa.cc.seg.dns.utils;

import java.util.Base64;

import org.springframework.stereotype.Component;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Component
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DemoHelper {

    private static byte[] encKey;
    private static byte[] hmacKey;

    public static byte[] getEncKey() {
        if (encKey == null) {
            encKey = Base64.getDecoder().decode("DJXkb7GyuXP5Hfep9OLukQ==");
        }

        return encKey;
    }

    public static byte[] getHmacKey() {
        if (hmacKey != null) {
            hmacKey = Base64.getDecoder().decode("QYp+xG2d7Ir8Xo2ZyD7m8FJKwrFrxd9ayN9i4mBQlTg=");
        }

        return hmacKey;
    }

}
