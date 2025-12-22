package com.example.smart_attendance_system;

public class ClassLocation {
    public String id;
    public String roomName;

    public ClassLocation(String id, String roomName) {
        this.id = id;
        this.roomName = roomName;
    }

    @Override
    public String toString() {
        return roomName; // Spinner shows this text
    }
}
