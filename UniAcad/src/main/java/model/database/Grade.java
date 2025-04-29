package model.database;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "Grade", schema = "dbo")
public class Grade {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "GradeID", nullable = false)
    private Integer id;

    @Column(name = "GradeName")
    private String gradeName;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CourseID", nullable = false)
    private Course courseID;

    @Column(name = "GradePercent", nullable = false)
    private Integer gradePercent;

    @OneToMany(mappedBy = "gradeID")
    private Set<Exam> exams = new LinkedHashSet<>();

    @OneToMany(mappedBy = "gradeID")
    private Set<Mark> marks = new LinkedHashSet<>();

}