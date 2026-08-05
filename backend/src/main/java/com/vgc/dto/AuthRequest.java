package com.vgc.dto;

public class AuthRequest {
    private String email;
    private String password;
    private String nickname;
    private String name;
    private Integer birthMonth;
    private Integer birthDay;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getBirthMonth() { return birthMonth; }
    public void setBirthMonth(Integer birthMonth) { this.birthMonth = birthMonth; }
    public Integer getBirthDay() { return birthDay; }
    public void setBirthDay(Integer birthDay) { this.birthDay = birthDay; }
}
