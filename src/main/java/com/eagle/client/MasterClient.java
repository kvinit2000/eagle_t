package com.eagle.client;

public class MasterClient {
    public static void main(String[] args) {
        while (true) {
            try {
                // 1) Ping
                System.out.println("PING: " + PingClient.ping());

                // 2) Signup (random user each cycle)
                String signupResponse = SignupClient.signupRandom();
                System.out.println("SIGNUP RESPONSE: " + signupResponse);

                // 3) List users
                System.out.println("LIST USERS: " + ListUsersClient.listUsers());

                System.out.println("--------------------------------------------------");
                Thread.sleep(5000);
            } catch (Exception e) {
                System.err.println("MasterClient error: " + e.getMessage());
                try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
            }
        }
    }
}
