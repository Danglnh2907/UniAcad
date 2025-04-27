package model.database;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "Department", schema = "dbo")
public class Department {
    @Id
    @Column(name = "DepartmentID", nullable = false, length = 2)
    private String departmentID;

    @Column(name = "DepartmentName", nullable = false, length = 100)
    private String departmentName;

    @OneToMany(mappedBy = "departmentID")
    private Set<Subject> subjects = new LinkedHashSet<>();

    @OneToMany(mappedBy = "departmentID")
    private Set<Teacher> teachers = new LinkedHashSet<>();

}