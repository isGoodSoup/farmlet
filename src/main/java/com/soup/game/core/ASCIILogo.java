package com.soup.game.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@SuppressWarnings("all")
public class ASCIILogo {
    public static void print() {
        try(var in = ASCIILogo.class.getResourceAsStream("/logo.txt")) {
            assert in != null;
            try(var reader = new BufferedReader(new InputStreamReader(in))) {
                String line;
                while((line = reader.readLine()) != null) {
                    System.out.println(line);
                    Thread.sleep(800);
                }
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
