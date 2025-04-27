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
public class GroupId implements Serializable {
    private static final long serialVersionUID = 3468431770185288165L;
    @Column(name = "StudentID", nullable = false, length = 8)
    private String studentID;

    @Column(name = "ClassID", nullable = false, length = 6)
    private String classID;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        GroupId entity = (GroupId) o;
        return Objects.equals(this.studentID, entity.studentID) &&
                Objects.equals(this.classID, entity.classID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(studentID, classID);
    }

}