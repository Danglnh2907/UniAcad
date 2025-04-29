package dao;

import model.database.Fee;
import model.database.Student;
import model.database.Term;
import util.service.database.DBContext;

import java.sql.*;

public class FeeDAO {

    public Fee findById(Integer id) {
        String sql = "SELECT * FROM Fee WHERE FeeID = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToFee(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding Fee by ID", e);
        }
        return null;
    }

    public void save(Fee fee) {
        String sql = "INSERT INTO Fee (StudentID, TermID, Amount, DueDate, FeeStatus) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, fee.getStudentID().getStudentID());
            if (fee.getTermID() != null) {
                ps.setString(2, fee.getTermID().getTermID());
            } else {
                ps.setNull(2, Types.CHAR);
            }
            ps.setBigDecimal(3, fee.getAmount());
            if (fee.getDueDate() != null) {
                ps.setTimestamp(4, Timestamp.from(fee.getDueDate()));
            } else {
                ps.setNull(4, Types.TIMESTAMP);
            }
            ps.setInt(5, fee.getFeeStatus());

            ps.executeUpdate();

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    fee.setId(generatedKeys.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error saving Fee", e);
        }
    }

    public void update(Fee fee) {
        String sql = "UPDATE Fee SET StudentID = ?, TermID = ?, Amount = ?, DueDate = ?, FeeStatus = ? WHERE FeeID = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, fee.getStudentID().getStudentID());
            if (fee.getTermID() != null) {
                ps.setString(2, fee.getTermID().getTermID());
            } else {
                ps.setNull(2, Types.CHAR);
            }
            ps.setBigDecimal(3, fee.getAmount());
            if (fee.getDueDate() != null) {
                ps.setTimestamp(4, Timestamp.from(fee.getDueDate()));
            } else {
                ps.setNull(4, Types.TIMESTAMP);
            }
            ps.setInt(5, fee.getFeeStatus());
            ps.setInt(6, fee.getId());

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating Fee", e);
        }
    }

    public Fee findUnpaidFeeByStudentId(String studentId) {
        String sql = "SELECT * FROM Fee WHERE StudentID = ? AND FeeStatus = 0";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToFee(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding unpaid Fee by StudentID", e);
        }
        return null;
    }

    private Fee mapResultSetToFee(ResultSet rs) throws SQLException {
        Fee fee = new Fee();
        fee.setId(rs.getInt("FeeID"));

        Student student = new Student();
        student.setStudentID(rs.getString("StudentID"));
        fee.setStudentID(student);

        Term term = new Term();
        term.setTermID(rs.getString("TermID"));
        fee.setTermID(term);

        fee.setAmount(rs.getBigDecimal("Amount"));

        Timestamp dueDateTimestamp = rs.getTimestamp("DueDate");
        if (dueDateTimestamp != null) {
            fee.setDueDate(dueDateTimestamp.toInstant());
        }
        fee.setFeeStatus(rs.getInt("FeeStatus"));
        return fee;
    }
}
