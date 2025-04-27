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
public class GradeReportId implements Serializable {
    private static final long serialVersionUID = 4516485438647714635L;
    @Column(name = "SubjectID", nullable = false, length = 7)
    private String subjectID;

    @Column(name = "StudentID", nullable = false, length = 8)
    private String studentID;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        GradeReportId entity = (GradeReportId) o;
        return Objects.equals(this.studentID, entity.studentID) &&
                Objects.equals(this.subjectID, entity.subjectID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(studentID, subjectID);
    }

}