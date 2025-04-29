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
@Table(name = "Teacher", schema = "dbo", uniqueConstraints = {
        @UniqueConstraint(name = "UQ__Teacher__4A75D54CB13D7A33", columnNames = {"TeacherEmail"})
})
public class Teacher {
    @Id
    @Column(name = "TeacherID", nullable = false, length = 20)
    private String teacherID;

    @Column(name = "TeacherEmail", nullable = false, length = 50)
    private String teacherEmail;

    @Column(name = "TeacherName", nullable = false, length = 100)
    private String teacherName;

    @Column(name = "TeacherPhone", nullable = false, length = 20)
    private String teacherPhone;

    @ColumnDefault("0")
    @Column(name = "TeacherStatus")
    private Integer teacherStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DepartmentID")
    private Department departmentID;

    @OneToMany(mappedBy = "teacherID")
    private Set<Slot> slots = new LinkedHashSet<>();

}