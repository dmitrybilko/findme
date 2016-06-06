package com.bilko.findme.models;

public class UserLocation {

    private Double longitude;
    private Double latitude;

    public UserLocation() {}

    public UserLocation(final Double longitude, final Double latitude) {
        this.longitude = longitude;
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLongitude(final Double longitude) {
        this.longitude = longitude;
    }

    public void setLatitude(final Double latitude) {
        this.latitude = latitude;
    }
}
