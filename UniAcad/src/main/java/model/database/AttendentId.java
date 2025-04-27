package model.database;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.Hibernate;

import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@Embeddable
public class AttendentId implements Serializable {
    public AttendentId(String studentID, Integer courseID, Integer slotNumber) {
        this.studentID = studentID;
        this.courseID = courseID;
        this.slotNumber = slotNumber;
    }

    private static final long serialVersionUID = -3383283253138944237L;
    @Column(name = "StudentID", nullable = false, length = 8)
    private String studentID;

    @Column(name = "CourseID", nullable = false)
    private Integer courseID;

    @Column(name = "SlotNumber", nullable = false)
    private Integer slotNumber;

    public AttendentId() {

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        AttendentId entity = (AttendentId) o;
        return Objects.equals(this.studentID, entity.studentID) &&
                Objects.equals(this.slotNumber, entity.slotNumber) &&
                Objects.equals(this.courseID, entity.courseID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(studentID, slotNumber, courseID);
    }

}