package model.database;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "\"Include\"", schema = "dbo")
public class Include {
    @EmbeddedId
    private IncludeId id;

    @MapsId("curriculumID")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CurriculumID", nullable = false)
    private Curriculum curriculumID;

    @MapsId("subjectID")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "SubjectID", nullable = false)
    private Subject subjectID;

    @Column(name = "Semester", nullable = false)
    private Integer semester;

}