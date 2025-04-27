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
    private static final long serialVersionUID = 815362830333514041L;
    @Column(name = "ClassID", nullable = false, length = 6)
    private String classID;

    @Column(name = "CourseID", nullable = false)
    private Integer courseID;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        StudyId entity = (StudyId) o;
        return Objects.equals(this.classID, entity.classID) &&
                Objects.equals(this.courseID, entity.courseID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(classID, courseID);
    }

}