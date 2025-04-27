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
        @UniqueConstraint(name = "UQ__Student__BF5D2EF57915C17E", columnNames = {"StudentEmail", "StudentSSN"})
})
public class Student {
    public Student(String studentID, String studentEmail, String studentName, LocalDate studentDoB, Boolean studentGender, String studentSSN, String address, Integer studentStatus, String studentPhone, Curriculum curriculumID) {
        this.studentID = studentID;
        this.studentEmail = studentEmail;
        this.studentName = studentName;
        this.studentDoB = studentDoB;
        this.studentGender = studentGender;
        this.studentSSN = studentSSN;
        this.address = address;
        this.studentPhone = studentPhone;
        this.studentStatus = studentStatus;
        this.curriculumID = curriculumID;
    }

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
    private Set<Grade> grades = new LinkedHashSet<>();

    @OneToMany(mappedBy = "studentID")
    private Set<GradeReport> gradeReports = new LinkedHashSet<>();

    @ManyToMany
    private Set<Class> classFields = new LinkedHashSet<>();

    public Student() {

    }
}