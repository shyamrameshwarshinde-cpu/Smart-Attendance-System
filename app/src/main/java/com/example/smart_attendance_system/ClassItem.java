package com.example.smart_attendance_system;

public class ClassItem {

    public String classId;
    public String displayText;

    public ClassItem(String classId, String displayText) {
        this.classId = classId;
        this.displayText = displayText;
    }

    // 🔑 THIS FIXES THE LISTVIEW CRASH
    @Override
    public String toString() {
        return displayText;
    }
}
