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
public class StudyId implements Serializable {
    private static final long serialVersionUID = 1645305416847177702L;
    @Column(name = "StudentID", nullable = false, length = 8)
    private String studentID;

    @Column(name = "CourseID", nullable = false)
    private Integer courseID;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        StudyId entity = (StudyId) o;
        return Objects.equals(this.studentID, entity.studentID) &&
                Objects.equals(this.courseID, entity.courseID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(studentID, courseID);
    }

}