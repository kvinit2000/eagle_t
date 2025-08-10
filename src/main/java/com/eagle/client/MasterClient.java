package com.eagle.client;

import com.eagle.model.response.UserProfileResponse;

import java.util.Map;
import java.util.Random;

public class MasterClient {
    private static final Random RAND = new Random();
    private static long ITER = 0; // loop counter

    public static void main(String[] args) {
        while (true) {
            try {
                ITER++;

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

                        // 2c) ACCOUNTS FLOW for the last user
                        // Create account (server will generate number)
                        String created = AccountsClient.create(SignupClient.LAST_AUTH_TOKEN, null);
                        System.out.println("ACCOUNT CREATE: " + created);

                        // List accounts
                        String listed = AccountsClient.list(SignupClient.LAST_AUTH_TOKEN);
                        System.out.println("ACCOUNT LIST: " + listed);

                        // Take the first account id from the list (helper parses JSON)
                        Integer firstId = AccountsClient.firstIdFromList(listed);
                        if (firstId != null) {
                            // Transactions demo on that account
                            String depAmt = String.format("%d.00", 10 + RAND.nextInt(91));    // 10..100
                            String withAmt = String.format("%d.00", 1 + RAND.nextInt(9));    // 1..9

                            String dep = AccountsClient.deposit(SignupClient.LAST_AUTH_TOKEN, firstId, depAmt);
                            System.out.println("TX DEPOSIT (" + depAmt + ") -> " + dep);

                            String wdr = AccountsClient.withdraw(SignupClient.LAST_AUTH_TOKEN, firstId, withAmt);
                            System.out.println("TX WITHDRAW (" + withAmt + ") -> " + wdr);

                            String txList = AccountsClient.listTransactions(SignupClient.LAST_AUTH_TOKEN, firstId);
                            System.out.println("TX LIST -> " + txList);

                            // Occasionally delete (every 7th cycle) so lists mostly grow
                            if (ITER % 7 == 0) {
                                String one = AccountsClient.getOne(SignupClient.LAST_AUTH_TOKEN, firstId);
                                System.out.println("ACCOUNT GET " + firstId + ": " + one);

                                String del = AccountsClient.delete(SignupClient.LAST_AUTH_TOKEN, firstId);
                                System.out.println("ACCOUNT DELETE " + firstId + ": " + del);
                            } else {
                                System.out.println("Skipping delete this cycle (ITER=" + ITER + ").");
                            }
                        } else {
                            System.out.println("No accounts found to transact on this cycle.");
                        }

                    } catch (Exception ex) {
                        System.err.println("[MasterClient] GET/PATCH/ACCOUNTS/TX (last) failed: " + ex.getMessage());
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

                        // Accounts: create and list for each (no delete here so lists can grow)
                        String created = AccountsClient.create(token, null);
                        System.out.println("ACCOUNT CREATE (" + username + "): " + created);

                        String listed = AccountsClient.list(token);
                        System.out.println("ACCOUNT LIST (" + username + "): " + listed);

                        // Optional: run a tiny transaction on first account if present
                        Integer firstId = AccountsClient.firstIdFromList(listed);
                        if (firstId != null) {
                            String depAmt = "15.00";
                            String dep = AccountsClient.deposit(token, firstId, depAmt);
                            System.out.println("TX DEPOSIT (" + username + ", " + depAmt + "): " + dep);

                            String txList = AccountsClient.listTransactions(token, firstId);
                            System.out.println("TX LIST (" + username + "): " + txList);
                        }

                    } catch (Exception ex) {
                        System.err.println("[MasterClient] /users/me flow failed for " + username + ": " + ex.getMessage());
                    }
                }

                // 4) List users
                System.out.println("LIST USERS: " + ListUsersClient.listUsers());

                System.out.println("--------------------------------------------------");
                Thread.sleep(10000);
            } catch (Exception e) {
                System.err.println("[MasterClient] error: " + e.getMessage());
                try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
            }
        }
    }
}
