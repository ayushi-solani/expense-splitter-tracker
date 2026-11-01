package com.ayushi.expensetracker.dto;

public class AuthResponse {
    private long userId;
    private String email;
    private String name;
    private String message;
    private String token;

    public AuthResponse(Long userId, String email, String name, String message, String token) {
        this.userId = userId;
        this.email = email;
        this.name = name;
        this.message = message;
        this.token = token;
    }
    public long getUserId() {return userId;}
    public String getEmail() {return email;}
    public String getName() {return name;}
    public String getMessage() {return message;}
    public String getToken() {return token;}
}
