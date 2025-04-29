package model.database;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalTime;

@Getter
@Setter
@Entity
@Table(name = "Exam", schema = "dbo")
public class Exam {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ExamID", nullable = false)
    private Integer id;

    @Column(name = "ExamName")
    private String examName;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "GradeID", nullable = false)
    private Grade gradeID;

    @Column(name = "ExamDate", nullable = false)
    private Instant examDate;

    @Column(name = "ExamDuration", nullable = false)
    private LocalTime examDuration;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RoomID")
    private Room roomID;

    @Column(name = "ExamType")
    private Integer examType;

}