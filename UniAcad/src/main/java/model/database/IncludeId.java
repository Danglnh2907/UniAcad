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
public class IncludeId implements Serializable {
    private static final long serialVersionUID = -7671041367492606076L;
    @Column(name = "CurriculumID", nullable = false, length = 50)
    private String curriculumID;

    @Column(name = "SubjectID", nullable = false, length = 7)
    private String subjectID;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        IncludeId entity = (IncludeId) o;
        return Objects.equals(this.curriculumID, entity.curriculumID) &&
                Objects.equals(this.subjectID, entity.subjectID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(curriculumID, subjectID);
    }

}