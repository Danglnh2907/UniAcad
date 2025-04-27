package model.database;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "Class", schema = "dbo")
public class Class {
    @Id
    @Column(name = "ClassID", nullable = false, length = 6)
    private String classID;

    @ManyToMany
    @JoinTable(name = "Group",
            joinColumns = @JoinColumn(name = "ClassID"),
            inverseJoinColumns = @JoinColumn(name = "StudentID"))
    private Set<Student> students = new LinkedHashSet<>();

    @ManyToMany
    @JoinTable(name = "Study",
            joinColumns = @JoinColumn(name = "ClassID"),
            inverseJoinColumns = @JoinColumn(name = "CourseID"))
    private Set<Course> courses = new LinkedHashSet<>();

}