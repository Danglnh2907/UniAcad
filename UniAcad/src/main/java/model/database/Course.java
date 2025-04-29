package model.database;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "Course", schema = "dbo")
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CourseID", nullable = false)
    private Integer id;

    @Column(name = "ClassID", length = 6)
    private String classID;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "SubjectID", nullable = false)
    private Subject subjectID;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "TermID", nullable = false)
    private Term termID;

    @Column(name = "TotalSlot", nullable = false)
    private Integer totalSlot;

    @ColumnDefault("0")
    @Column(name = "CourseStatus", nullable = false)
    private Boolean courseStatus = false;

    @OneToMany(mappedBy = "courseID")
    private Set<Grade> grades = new LinkedHashSet<>();

    @OneToMany(mappedBy = "courseID")
    private Set<Slot> slots = new LinkedHashSet<>();

    @ManyToMany
    @JoinTable(name = "Study",
            joinColumns = @JoinColumn(name = "CourseID"),
            inverseJoinColumns = @JoinColumn(name = "StudentID"))
    private Set<Student> students = new LinkedHashSet<>();

}