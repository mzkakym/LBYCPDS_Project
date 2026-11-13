package org.example.lbycpds_project.models;

public class User {
    private String userID;
    private String password;
    private String role;
    private String fullName;
    private String department;
    private String contact;
    private String status;
    public User(String userID, String password, String role, String fullName, String department, String contact, String status) {
        this.userID = userID;
        this.password = password;
        this.role = role;
        this.fullName = fullName;
        this.department = department;
        this.contact = contact;
        this.status = status;
    }

    public String getUserID() {
        return userID;
    }

    public String getPassword() {
        return password;
    }

    public String getRole() {
        return role;
    }

    public String getFullName() {
        return fullName;
    }

    public String getDepartment() {
        return department;
    }

    public String getContact() {
        return contact;
    }

    public String getStatus() {
        return status;
    }
}