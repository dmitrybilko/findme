package com.bilko.findme.models;

public class User {

    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private UserLocation userLocation;

    public User() {}

    public User(final String firstName, final String lastName, final String email,
        final String password) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public UserLocation getUserLocation() {
        return userLocation;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public void setFirstName(final String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(final String lastName) {
        this.lastName = lastName;
    }

    public void setEmail(final String email) {
        this.email = email;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    public void setUserLocation(final UserLocation userLocation) {
        this.userLocation = userLocation;
    }
}
