package dao;

import model.database.Room;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.service.database.DBContext;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RoomDAO extends DBContext {
    private static final Logger logger = LoggerFactory.getLogger(RoomDAO.class);

    public RoomDAO() {
        super();
    }

    private Room mapResult(ResultSet rs) throws SQLException {
        Room room = new Room();
        room.setRoomID(rs.getString("RoomID"));
        return room;
    }

    public Room getRoomById(String roomId) {
        String query = "SELECT * FROM Room WHERE RoomID = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setString(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResult(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Error retrieving room by ID: {}", roomId, e);
        }
        return null;
    }

    public List<Room> getAllRooms() {
        List<Room> rooms = new ArrayList<>();
        String query = "SELECT * FROM Room";
        try (PreparedStatement ps = getConnection().prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rooms.add(mapResult(rs));
            }
        } catch (SQLException e) {
            logger.error("Error retrieving all rooms", e);
        }
        return rooms;
    }

    public boolean createRoom(Room room) {
        String query = "INSERT INTO Room (RoomID) VALUES (?)";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setString(1, room.getRoomID());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error creating room: {}", room.getRoomID(), e);
        }
        return false;
    }

    public boolean updateRoom(Room room) {
        // Room chỉ có RoomID => update không cần thiết.
        logger.warn("Update Room is not supported because Room only has RoomID.");
        return false;
    }

    public boolean deleteRoom(String roomId) {
        String query = "DELETE FROM Room WHERE RoomID = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setString(1, roomId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error deleting room: {}", roomId, e);
        }
        return false;
    }
}
