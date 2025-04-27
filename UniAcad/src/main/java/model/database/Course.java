package model.database;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

@Getter
@Setter
@Entity
@Table(name = "Course", schema = "dbo")
public class Course {
    @Id
    @Column(name = "CourseID", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "SubjectID", nullable = false)
    private Subject subjectID;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "TermID", nullable = false)
    private Term termID;

    @ColumnDefault("0")
    @Column(name = "CourseStatus", nullable = false)
    private Boolean courseStatus = false;


    public void setCourseID(int courseID) {
        this.id = courseID;
    }
}