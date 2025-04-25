package model;

public class Room {
    private String roomId; // CHAR(4)

    // Constructor
    public Room() {
    }

    public Room(String roomId) {
        this.roomId = roomId;
    }

    // Getters and Setters
    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }
}