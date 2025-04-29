package model.database;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "Mark", schema = "dbo")
public class Mark {
    @EmbeddedId
    private MarkId id;

    @MapsId("gradeID")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "GradeID", nullable = false)
    private Grade gradeID;

    @MapsId("studentID")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "StudentID", nullable = false)
    private Student studentID;

    @Column(name = "Mark", precision = 3, scale = 1)
    private BigDecimal mark;

}