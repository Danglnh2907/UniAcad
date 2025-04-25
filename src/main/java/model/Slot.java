package model;

import java.sql.Time;
import java.sql.Date;

public class Slot {
    private int slotNumber; // INT
    private Time startTime; // TIME
    private Time duration; // TIME
    private Date date; // DATE
    private int courseId; // INT, Foreign Key to Course
    private String teacherId; // VARCHAR, Foreign Key to Teacher
    private String roomId; // CHAR(4), Foreign Key to Room

    // Constructor
    public Slot() {
    }

    public Slot(int slotNumber, Time startTime, Time duration, Date date, int courseId, String teacherId, String roomId) {
        this.slotNumber = slotNumber;
        this.startTime = startTime;
        this.duration = duration;
        this.date = date;
        this.courseId = courseId;
        this.teacherId = teacherId;
        this.roomId = roomId;
    }

    // Getters and Setters
    public int getSlotNumber() {
        return slotNumber;
    }

    public void setSlotNumber(int slotNumber) {
        this.slotNumber = slotNumber;
    }

    public Time getStartTime() {
        return startTime;
    }

    public void setStartTime(Time startTime) {
        this.startTime = startTime;
    }

    public Time getDuration() {
        return duration;
    }

    public void setDuration(Time duration) {
        this.duration = duration;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public int getCourseId() {
        return courseId;
    }

    public void setCourseId(int courseId) {
        this.courseId = courseId;
    }

    public String getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(String teacherId) {
        this.teacherId = teacherId;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }
}