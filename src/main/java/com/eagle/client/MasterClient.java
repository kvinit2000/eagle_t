package com.eagle.client;

import com.eagle.model.response.UserProfileResponse;

import java.util.Map;
import java.util.Random;

public class MasterClient {
    private static final Random RAND = new Random();

    public static void main(String[] args) {
        while (true) {
            try {
                // 1) Ping
                System.out.println("PING: " + PingClient.ping());

                // 2) Signup new random user
                String signupResponse = SignupClient.signupRandom();
                System.out.println("SIGNUP RESPONSE: " + signupResponse);

                // 2a) GET /users/me for the last signed-up user (using the signup authToken)
                if (SignupClient.LAST_AUTH_TOKEN != null && !SignupClient.LAST_AUTH_TOKEN.isBlank()) {
                    try {
                        UserProfileResponse me = MeClient.getMe(SignupClient.LAST_AUTH_TOKEN);
                        System.out.println("ME (last): username=" + me.getUsername()
                                + ", email=" + me.getEmail()
                                + ", dob=" + me.getDob());

                        // 2b) PATCH /users/me for the last user (demo: update email & address)
                        String newEmail = "updated+" + System.currentTimeMillis() + "@example.com";
                        String newAddress = "Unit " + (100 + RAND.nextInt(900)) + ", Updated Street";
                        UserProfileResponse after = MeClient.patchMe(
                                SignupClient.LAST_AUTH_TOKEN,
                                newEmail,   // email
                                null,       // dob (leave unchanged)
                                newAddress, // address
                                null,       // pin
                                null        // phone
                        );
                        System.out.println("ME (last after patch): email=" + after.getEmail()
                                + ", address=" + after.getAddress());

                    } catch (Exception ex) {
                        System.err.println("[MasterClient] /users/me GET or PATCH (last) failed: " + ex.getMessage());
                    }
                } else {
                    System.out.println("ME (last): token not available");
                }

                // 3) Login all stored users (existing flow)
                for (Map.Entry<String, String> entry : SignupClient.USERS.entrySet()) {
                    String loginResponse = LoginClient.login(entry.getKey(), entry.getValue());
                    System.out.println("LOGIN (" + entry.getKey() + "): " + loginResponse);
                    // If LoginClient later returns tokens, parse & store them into SignupClient.TOKENS here.
                }

                // 3a) GET + PATCH /users/me for all users we have tokens for (from signup captures)
                for (Map.Entry<String, String> e : SignupClient.TOKENS.entrySet()) {
                    String username = e.getKey();
                    String token = e.getValue();
                    try {
                        UserProfileResponse me = MeClient.getMe(token);
                        System.out.println("ME (" + username + "): username=" + me.getUsername()
                                + ", email=" + me.getEmail()
                                + ", dob=" + me.getDob());

                        // Demo: update phone only for each stored user
                        String newPhone = "+44" + (100000000 + RAND.nextInt(899999999));
                        UserProfileResponse after = MeClient.patchMe(
                                token,
                                null,   // email
                                null,   // dob
                                null,   // address
                                null,   // pin
                                newPhone
                        );
                        System.out.println("ME (" + username + " after patch): phone=" + after.getPhone());
                    } catch (Exception ex) {
                        System.err.println("[MasterClient] /users/me GET or PATCH failed for " + username + ": " + ex.getMessage());
                    }
                }

                // 4) List users
                System.out.println("LIST USERS: " + ListUsersClient.listUsers());

                System.out.println("--------------------------------------------------");
                Thread.sleep(5000);
            } catch (Exception e) {
                System.err.println("[MasterClient] error: " + e.getMessage());
                try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
            }
        }
    }
}
