package com.osuobiem.mobiletracker.track;

public class DeviceLocation {
    private double latitude;
    private double longitude;

    public DeviceLocation(){}

    public DeviceLocation(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }
}
