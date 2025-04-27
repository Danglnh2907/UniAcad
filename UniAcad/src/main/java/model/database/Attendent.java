package model.database;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

@Getter
@Setter
@Entity
@Table(name = "Attendent", schema = "dbo")
public class Attendent {
    @EmbeddedId
    private AttendentId id;

    @MapsId("studentID")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "StudentID", nullable = false)
    private Student studentID;

    @MapsId("id")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumns({
            @JoinColumn(name = "SlotNumber", referencedColumnName = "SlotNumber", nullable = false),
            @JoinColumn(name = "CourseID", referencedColumnName = "CourseID", nullable = false)
    })
    private Slot slot;

    @ColumnDefault("NULL")
    @Column(name = "Status")
    private Boolean status;

}