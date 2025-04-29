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
public class MarkId implements Serializable {
    private static final long serialVersionUID = 6344329259969856309L;
    @Column(name = "GradeID", nullable = false)
    private Integer gradeID;

    @Column(name = "StudentID", nullable = false, length = 8)
    private String studentID;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        MarkId entity = (MarkId) o;
        return Objects.equals(this.studentID, entity.studentID) &&
                Objects.equals(this.gradeID, entity.gradeID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(studentID, gradeID);
    }

}