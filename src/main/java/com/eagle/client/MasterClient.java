package com.eagle.client;

import com.eagle.model.response.UserProfileResponse;

import java.util.Map;
import java.util.Random;

public class MasterClient {
    private static final Random RAND = new Random();
    private static final String BASE_URL = "http://localhost:8080";
    private static final long SLEEP_MS = 10_000;      // pause between cycles
    private static final long ERROR_PAUSE_MS = 3_000; // pause after error

    private static long ITER = 0; // loop counter

    public static void main(String[] args) {
        while (true) {
            try {
                ITER++;
                System.out.println("\n==== ITER " + ITER + " ====");

                // 1) Ping
                try {
                    String pong = PingClient.ping(BASE_URL);
                    System.out.println("PING: " + pong);
                } catch (Exception e) {
                    System.err.println("PING failed: " + e.getMessage());
                }

                // 2) Signup new random user
                String signupResponse = null;
                try {
                    signupResponse = SignupClient.signupRandom(BASE_URL);
                    System.out.println("SIGNUP RESPONSE: " + signupResponse);
                } catch (Exception e) {
                    System.err.println("SIGNUP failed: " + e.getMessage());
                }

                // 2a) GET /users/me for the last signed-up user (using the signup authToken)
                if (SignupClient.LAST_AUTH_TOKEN != null && !SignupClient.LAST_AUTH_TOKEN.isBlank()) {
                    String token = SignupClient.LAST_AUTH_TOKEN;
                    try {
                        UserProfileResponse me = MeClient.getMe(BASE_URL, token);
                        if (me != null) {
                            System.out.println("ME (last): username=" + me.getUsername()
                                    + ", email=" + me.getEmail()
                                    + ", dob=" + me.getDob());
                        }

                        // 2b) PATCH /users/me for the last user (demo: update email & address)
                        String newEmail = "updated+" + System.currentTimeMillis() + "@example.com";
                        String newAddress = "Unit " + (100 + RAND.nextInt(900)) + ", Updated Street";
                        UserProfileResponse after = MeClient.patchMe(
                                BASE_URL,
                                token,
                                newEmail,   // email
                                null,       // dob (leave unchanged)
                                newAddress, // address
                                null,       // pin
                                null        // phone
                        );
                        if (after != null) {
                            System.out.println("ME (last after patch): email=" + after.getEmail()
                                    + ", address=" + after.getAddress());
                        }

                        // 2c) ACCOUNTS FLOW for the last user
                        // Create account (server will generate number)
                        try {
                            String created = AccountsClient.create(token, null);
                            System.out.println("ACCOUNT CREATE: " + created);

                            // List accounts
                            String listed = AccountsClient.list(token);
                            System.out.println("ACCOUNT LIST: " + listed);

                            // Take the first account id from the list (helper parses JSON)
                            Integer firstId = AccountsClient.firstIdFromList(listed);
                            if (firstId != null) {
                                // Transactions demo on that account
                                String depAmt = String.format("%d.00", 10 + RAND.nextInt(91));    // 10..100
                                String withAmt = String.format("%d.00", 1 + RAND.nextInt(9));     // 1..9

                                String dep = AccountsClient.deposit(token, firstId, depAmt);
                                System.out.println("TX DEPOSIT (" + depAmt + ") -> " + dep);

                                String wdr = AccountsClient.withdraw(token, firstId, withAmt);
                                System.out.println("TX WITHDRAW (" + withAmt + ") -> " + wdr);

                                String txList = AccountsClient.listTransactions(token, firstId, 50, 0);
                                System.out.println("TX LIST -> " + txList);

                                // Occasionally delete (every 7th cycle) so lists mostly grow
                                if (ITER % 7 == 0) {
                                    String one = AccountsClient.getOne(token, firstId);
                                    System.out.println("ACCOUNT GET " + firstId + ": " + one);

                                    String del = AccountsClient.delete(token, firstId);
                                    System.out.println("ACCOUNT DELETE " + firstId + ": " + del);
                                } else {
                                    System.out.println("Skipping delete this cycle (ITER=" + ITER + ").");
                                }
                            } else {
                                System.out.println("No accounts found to transact on this cycle.");
                            }
                        } catch (Exception e) {
                            System.err.println("[MasterClient] ACCOUNTS/TX (last) failed: " + e.getMessage());
                        }

                    } catch (Exception ex) {
                        System.err.println("[MasterClient] GET/PATCH (last) failed: " + ex.getMessage());
                    }
                } else {
                    System.out.println("ME (last): token not available");
                }

                // 3) Login all stored users (existing flow)
                for (Map.Entry<String, String> entry : SignupClient.USERS.entrySet()) {
                    try {
                        String loginResponse = LoginClient.login(entry.getKey(), entry.getValue());
                        System.out.println("LOGIN (" + entry.getKey() + "): " + loginResponse);
                        // If LoginClient later returns tokens, parse & store them into SignupClient.TOKENS here.
                    } catch (Exception e) {
                        System.err.println("LOGIN (" + entry.getKey() + ") failed: " + e.getMessage());
                    }
                }

                // 3a) GET + PATCH /users/me for all users we have tokens for (from signup captures)
                for (Map.Entry<String, String> e : SignupClient.TOKENS.entrySet()) {
                    String username = e.getKey();
                    String token = e.getValue();
                    try {
                        UserProfileResponse me = MeClient.getMe(BASE_URL, token);
                        if (me != null) {
                            System.out.println("ME (" + username + "): username=" + me.getUsername()
                                    + ", email=" + me.getEmail()
                                    + ", dob=" + me.getDob());
                        }

                        // Demo: update phone only for each stored user
                        String newPhone = "+44" + (100000000 + RAND.nextInt(899_999_999));
                        UserProfileResponse after = MeClient.patchMe(
                                BASE_URL,
                                token,
                                null,   // email
                                null,   // dob
                                null,   // address
                                null,   // pin
                                newPhone
                        );
                        if (after != null) {
                            System.out.println("ME (" + username + " after patch): phone=" + after.getPhone());
                        }

                        // Accounts: create and list for each (no delete here so lists can grow)
                        try {
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

                                String txList = AccountsClient.listTransactions(token, firstId, 10, 0);
                                System.out.println("TX LIST (" + username + "): " + txList);
                            }
                        } catch (Exception e1) {
                            System.err.println("[MasterClient] Accounts flow failed for " + username + ": " + e1.getMessage());
                        }

                    } catch (Exception ex) {
                        System.err.println("[MasterClient] /users/me flow failed for " + username + ": " + ex.getMessage());
                    }
                }

                // 4) List users
                try {
                    System.out.println("LIST USERS: " + ListUsersClient.listUsers());
                } catch (Exception e) {
                    System.err.println("LIST USERS failed: " + e.getMessage());
                }

                System.out.println("--------------------------------------------------");
                Thread.sleep(SLEEP_MS);
            } catch (Exception e) {
                System.err.println("[MasterClient] error: " + e.getMessage());
                try { Thread.sleep(ERROR_PAUSE_MS); } catch (InterruptedException ignored) {}
            }
        }
    }
}
