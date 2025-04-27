package model.database;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "GradeReport", schema = "dbo")
public class GradeReport {
    @EmbeddedId
    private GradeReportId id;

    @MapsId("subjectID")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "SubjectID", nullable = false)
    private Subject subjectID;

    @MapsId("studentID")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "StudentID", nullable = false)
    private Student studentID;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TermID")
    private Term termID;

    @Column(name = "Mark", precision = 3, scale = 1)
    private BigDecimal mark;

    @Column(name = "GradeReportStatus")
    private Integer gradeReportStatus;

}