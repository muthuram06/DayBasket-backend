
package com.daybasket.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.util.Scanner;

public class PasswordHashGenerator {

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter admin password: ");
        String password = scanner.nextLine();

        BCryptPasswordEncoder encoder =
                new BCryptPasswordEncoder();

        String hash = encoder.encode(password);

        System.out.println("BCrypt Hash:");
        System.out.println(hash);

        scanner.close();
    }
}
