package model.database;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "Grade", schema = "dbo")
public class Grade {
    @EmbeddedId
    private GradeId id;

    @MapsId("courseID")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CourseID", nullable = false)
    private Course courseID;

    @MapsId("studentID")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "StudentID", nullable = false)
    private Student studentID;

    @Column(name = "FE")
    private Integer fe;

    @Column(name = "OnGoing")
    private Integer onGoing;

}