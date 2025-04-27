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
@Table(name = "Subject", schema = "dbo")
public class Subject {
    @Id
    @Column(name = "SubjectID", nullable = false, length = 7)
    private String subjectID;

    @Column(name = "SubjectName", nullable = false, length = 100)
    private String subjectName;

    @Column(name = "Credits")
    private Integer credits;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DepartmentID")
    private Department departmentID;

    @ColumnDefault("0")
    @Column(name = "IsDeleted")
    private Boolean isDeleted;

    @OneToMany(mappedBy = "subjectID")
    private Set<Course> courses = new LinkedHashSet<>();

    @OneToMany(mappedBy = "subjectID")
    private Set<GradeReport> gradeReports = new LinkedHashSet<>();

    @OneToMany(mappedBy = "subjectID")
    private Set<Include> includes = new LinkedHashSet<>();

}