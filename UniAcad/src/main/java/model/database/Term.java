package model.database;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "Term", schema = "dbo")
public class Term {
    @Id
    @Column(name = "TermID", nullable = false, length = 4)
    private String termID;

    @Column(name = "TermName", nullable = false, length = 50)
    private String termName;

    @OneToMany(mappedBy = "termID")
    private Set<Course> courses = new LinkedHashSet<>();

    @OneToMany(mappedBy = "termID")
    private Set<Fee> fees = new LinkedHashSet<>();

    @OneToMany(mappedBy = "termID")
    private Set<GradeReport> gradeReports = new LinkedHashSet<>();

}