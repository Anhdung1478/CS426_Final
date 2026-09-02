package com.lexicondepths.proxy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * DeepSeek proxy for Lexicon Depths. Exists for exactly one reason: the API key must not ship
 * inside the APK, where apktool would find it in a minute. Stateless — it forwards a prompt,
 * validates the reply, and forgets both.
 */
@SpringBootApplication
public class ProxyApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProxyApplication.class, args);
    }
}
