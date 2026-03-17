package com.vgc.dto;

public class AuthResponse {
    private String token;
    private String nickname;
    private String name;
    private String role;

    public AuthResponse(String token, String nickname, String name, String role) {
        this.token = token;
        this.nickname = nickname;
        this.name = name;
        this.role = role;
    }

    public String getToken() { return token; }
    public String getNickname() { return nickname; }
    public String getName() { return name; }
    public String getRole() { return role; }
}
