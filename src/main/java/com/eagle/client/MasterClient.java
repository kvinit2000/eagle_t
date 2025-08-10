package com.eagle.client;

public class MasterClient {
    public static void main(String[] args) throws Exception {
        while (true) {
            // 1. Ping
            System.out.println("PING: " + PingClient.ping());

            // 2. Signup
            String signupResponse = SignupClient.signupRandom();
            System.out.println("SIGNUP RESPONSE: " + signupResponse);

            // 3. List users
            System.out.println("LIST USERS: " + ListUsersClient.listUsers());

            System.out.println("--------------------------------------------------");
            Thread.sleep(5000);
        }
    }
}
