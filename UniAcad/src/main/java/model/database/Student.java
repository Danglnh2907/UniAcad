package model.database;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "Student", schema = "dbo", uniqueConstraints = {
        @UniqueConstraint(name = "UQ__Student__BF5D2EF573D6FC67", columnNames = {"StudentEmail", "StudentSSN"})
})
public class Student {
    @Id
    @Column(name = "StudentID", nullable = false, length = 8)
    private String studentID;

    @Column(name = "StudentEmail", nullable = false, length = 50)
    private String studentEmail;

    @Column(name = "StudentName", nullable = false, length = 100)
    private String studentName;

    @Column(name = "StudentDoB", nullable = false)
    private LocalDate studentDoB;

    @Column(name = "StudentGender", nullable = false)
    private Boolean studentGender = false;

    @Column(name = "StudentSSN", nullable = false, length = 20)
    private String studentSSN;

    @Column(name = "Address")
    private String address;

    @ColumnDefault("0")
    @Column(name = "StudentStatus")
    private Integer studentStatus;

    @Column(name = "StudentPhone", nullable = false, length = 20)
    private String studentPhone;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CurriculumID", nullable = false)
    private Curriculum curriculumID;

    @OneToMany(mappedBy = "studentID")
    private Set<Attendent> attendents = new LinkedHashSet<>();

    @OneToMany(mappedBy = "studentID")
    private Set<Fee> fees = new LinkedHashSet<>();

    @OneToMany(mappedBy = "studentID")
    private Set<GradeReport> gradeReports = new LinkedHashSet<>();

    @OneToMany(mappedBy = "studentID")
    private Set<Mark> marks = new LinkedHashSet<>();

    @ManyToMany
    private Set<Course> courses = new LinkedHashSet<>();

    public Student(String studentID, String studentName, String studentSSN, String studentEmail, String studentPhone, Curriculum curriculumID, Boolean studentGender, String address, LocalDate studentDoB, Integer studentStatus) {
        this.studentID = studentID;
        this.studentName = studentName;
        this.studentSSN = studentSSN;
        this.studentEmail = studentEmail;
        this.studentPhone = studentPhone;
        this.curriculumID = curriculumID;
        this.studentGender = studentGender;
        this.address = address;
        this.studentDoB = studentDoB;
        this.studentStatus = studentStatus;
    }

    public Student() {

    }
}