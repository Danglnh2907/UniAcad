package model.database;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "Study", schema = "dbo")
public class Study {
    @EmbeddedId
    private StudyId id;

    @MapsId("studentID")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "StudentID", nullable = false)
    private Student studentID;

    @MapsId("courseID")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CourseID", nullable = false)
    private Course courseID;

}