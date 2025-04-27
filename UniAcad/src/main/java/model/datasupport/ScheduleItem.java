package model.datasupport;

import java.sql.Time;
import java.sql.Timestamp;

public class ScheduleItem {
    private String subjectName;
    private String roomId;
    private java.sql.Timestamp startTime;
    private java.sql.Time duration;
    private Boolean attendStatus; // true, false hoặc null (chưa điểm danh)

    public ScheduleItem(String subjectName, String roomId, java.sql.Timestamp startTime, java.sql.Time duration, Boolean attendStatus) {
        this.subjectName = subjectName;
        this.roomId = roomId;
        this.startTime = startTime;
        this.duration = duration;
        this.attendStatus = attendStatus;
    }

    public ScheduleItem(String subjectName, String roomId, Timestamp startTime, Time duration) {
        this.subjectName = subjectName;
        this.roomId = roomId;
        this.startTime = startTime;
        this.duration = duration;
    }

    // Getter và Setter
    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public java.sql.Timestamp getStartTime() { return startTime; }
    public void setStartTime(java.sql.Timestamp startTime) { this.startTime = startTime; }

    public java.sql.Time getDuration() { return duration; }
    public void setDuration(java.sql.Time duration) { this.duration = duration; }

    public Boolean getAttendStatus() { return attendStatus; }
    public void setAttendStatus(Boolean attendStatus) { this.attendStatus = attendStatus; }
}
