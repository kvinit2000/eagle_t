package com.eagle.client;

import com.eagle.model.response.UserProfileResponse;

import java.util.Map;
import java.util.Random;

public class MasterClient {
    private static final Random RAND = new Random();
    private static final String BASE_URL = AbsClient.DEFAULT_BASE_URL;
    private static final long SLEEP_MS = 10_000;
    private static final long ERROR_PAUSE_MS = 3_000;

    private static long ITER = 0;

    public static void main(String[] args) {
        AccountsClient accounts = new AccountsClient();
        while (true) {
            try {
                ITER++;
                System.out.println("\n==== ITER " + ITER + " ====");

                // 1) Ping
                try { System.out.println("PING: " + PingClient.ping(BASE_URL)); }
                catch (Exception e) { System.err.println("PING failed: " + e.getMessage()); }

                // 2) Signup new random user
                try {
                    String signupResponse = new SignupClient().signupRandom(BASE_URL);
                    System.out.println("SIGNUP RESPONSE: " + signupResponse);
                } catch (Exception e) { System.err.println("SIGNUP failed: " + e.getMessage()); }

                // 2a) /users/me for last user
                if (SignupClient.LAST_AUTH_TOKEN != null && !SignupClient.LAST_AUTH_TOKEN.isBlank()) {
                    String token = SignupClient.LAST_AUTH_TOKEN;
                    try {
                        UserProfileResponse me = new MeClient().getMe(BASE_URL, token);
                        if (me != null) System.out.println("ME (last): username="+me.getUsername()+", email="+me.getEmail()+", dob="+me.getDob());

                        String newEmail = "updated+" + System.currentTimeMillis() + "@example.com";
                        String newAddress = "Unit " + (100 + RAND.nextInt(900)) + ", Updated Street";
                        UserProfileResponse after = new MeClient().patchMe(BASE_URL, token, newEmail, null, newAddress, null, null);
                        if (after != null) System.out.println("ME (last after patch): email="+after.getEmail()+", address="+after.getAddress());

                        // Accounts demo
                        try {
                            String created = accounts.create(BASE_URL, token, null);
                            System.out.println("ACCOUNT CREATE: " + created);
                            String listed = accounts.list(BASE_URL, token);
                            System.out.println("ACCOUNT LIST: " + listed);
                            Integer firstId = AccountsClient.firstIdFromList(listed);
                            if (firstId != null) {
                                String depAmt = String.format("%d.00", 10 + RAND.nextInt(91));
                                String withAmt = String.format("%d.00", 1 + RAND.nextInt(9));
                                System.out.println("TX DEPOSIT ("+depAmt+") -> " + accounts.deposit(BASE_URL, token, firstId, depAmt));
                                System.out.println("TX WITHDRAW ("+withAmt+") -> " + accounts.withdraw(BASE_URL, token, firstId, withAmt));
                                System.out.println("TX LIST -> " + accounts.listTransactions(BASE_URL, token, firstId, 50, 0));
                                if (ITER % 7 == 0) {
                                    System.out.println("ACCOUNT GET "+firstId+": "+accounts.getOne(BASE_URL, token, firstId));
                                    System.out.println("ACCOUNT DELETE "+firstId+": "+accounts.delete(BASE_URL, token, firstId));
                                } else {
                                    System.out.println("Skipping delete this cycle (ITER=" + ITER + ").");
                                }
                            } else {
                                System.out.println("No accounts found to transact on this cycle.");
                            }
                        } catch (Exception e) { System.err.println("[MasterClient] ACCOUNTS/TX (last) failed: " + e.getMessage()); }

                    } catch (Exception ex) {
                        System.err.println("[MasterClient] GET/PATCH (last) failed: " + ex.getMessage());
                    }
                } else {
                    System.out.println("ME (last): token not available");
                }

                // 3) Login all stored users
                for (Map.Entry<String,String> entry : SignupClient.USERS.entrySet()) {
                    try {
                        String loginResponse = new LoginClient().login(AbsClient.DEFAULT_BASE_URL, entry.getKey(), entry.getValue());
                        System.out.println("LOGIN ("+entry.getKey()+"): " + loginResponse);
                    } catch (Exception e) { System.err.println("LOGIN ("+entry.getKey()+") failed: " + e.getMessage()); }
                }

                // 3a) GET + PATCH for users with tokens
                for (Map.Entry<String,String> e : SignupClient.TOKENS.entrySet()) {
                    String username = e.getKey(), token = e.getValue();
                    try {
                        UserProfileResponse me = new MeClient().getMe(BASE_URL, token);
                        if (me != null) System.out.println("ME ("+username+"): username="+me.getUsername()+", email="+me.getEmail()+", dob="+me.getDob());
                        String newPhone = "+44" + (100000000 + RAND.nextInt(899_999_999));
                        UserProfileResponse after = new MeClient().patchMe(BASE_URL, token, null, null, null, null, newPhone);
                        if (after != null) System.out.println("ME ("+username+" after patch): phone="+after.getPhone());
                        String created = accounts.create(BASE_URL, token, null);
                        System.out.println("ACCOUNT CREATE ("+username+"): " + created);
                        String listed = accounts.list(BASE_URL, token);
                        System.out.println("ACCOUNT LIST ("+username+"): " + listed);
                        Integer firstId = AccountsClient.firstIdFromList(listed);
                        if (firstId != null) {
                            String depAmt = "15.00";
                            System.out.println("TX DEPOSIT ("+username+", "+depAmt+"): " + accounts.deposit(BASE_URL, token, firstId, depAmt));
                            System.out.println("TX LIST ("+username+"): " + accounts.listTransactions(BASE_URL, token, firstId, 10, 0));
                        }
                    } catch (Exception ex) {
                        System.err.println("[MasterClient] /users/me flow failed for " + username + ": " + ex.getMessage());
                    }
                }

                // 4) List users
                try { System.out.println("LIST USERS: " + new ListUsersClient().listUsers()); }
                catch (Exception e) { System.err.println("LIST USERS failed: " + e.getMessage()); }

                System.out.println("--------------------------------------------------");
                Thread.sleep(SLEEP_MS);
            } catch (Exception e) {
                System.err.println("[MasterClient] error: " + e.getMessage());
                try { Thread.sleep(ERROR_PAUSE_MS); } catch (InterruptedException ignored) {}
            }
        }
    }
}