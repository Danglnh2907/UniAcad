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
public class SlotId implements Serializable {
    private static final long serialVersionUID = 128948441692087001L;
    @Column(name = "SlotNumber", nullable = false)
    private Integer slotNumber;

    @Column(name = "CourseID", nullable = false)
    private Integer courseID;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        SlotId entity = (SlotId) o;
        return Objects.equals(this.slotNumber, entity.slotNumber) &&
                Objects.equals(this.courseID, entity.courseID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(slotNumber, courseID);
    }

}