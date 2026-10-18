package com.example.myapplication.model;

public class User {
    private int id;
    private int userId;
    private String fullName;
    private String email;
    private String password;
    private String memberLevel;
    private int streakDays;
    private double focusHours;
    private String phone;
    private String bio;
    private String avatarUrl;

    // --- GETTER ---
    public int getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getMemberLevel() { return memberLevel; }
    public int getStreakDays() { return streakDays; }
    public double getFocusHours() { return focusHours; }
    public String getPhone() { return phone; }
    public String getBio() { return bio; }
    public String getAvatarUrl() { return avatarUrl; }

    // --- SETTER ---
    public void setId(int id) { this.id = id; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setEmail(String email) { this.email = email; }
    public void setPassword(String password) { this.password = password; }
    public void setMemberLevel(String memberLevel) { this.memberLevel = memberLevel; }
    public void setStreakDays(int streakDays) { this.streakDays = streakDays; }
    public void setFocusHours(double focusHours) { this.focusHours = focusHours; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setBio(String bio) { this.bio = bio; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

}