package model.database;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "Curriculum", schema = "dbo")
public class Curriculum {
    @Id
    @Column(name = "CurriculumID", nullable = false, length = 50)
    private String curriculumID;

    @Column(name = "CurriculumName", nullable = false)
    private String curriculumName;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "MajorID", nullable = false)
    private Major majorID;

    @OneToMany(mappedBy = "curriculumID")
    private Set<Include> includes = new LinkedHashSet<>();

    @OneToMany(mappedBy = "curriculumID")
    private Set<Student> students = new LinkedHashSet<>();

}