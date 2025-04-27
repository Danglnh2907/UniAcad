package model.database;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "Slot", schema = "dbo")
public class Slot {
    @EmbeddedId
    private SlotId id;

    @MapsId("courseID")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CourseID", nullable = false)
    private Course courseID;

    @Column(name = "StartTime")
    private Instant startTime;

    @Column(name = "Duration")
    private LocalTime duration;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TeacherID")
    private Teacher teacherID;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RoomID")
    private Room roomID;

    @OneToMany(mappedBy = "slot")
    private Set<Attendent> attendents = new LinkedHashSet<>();

}