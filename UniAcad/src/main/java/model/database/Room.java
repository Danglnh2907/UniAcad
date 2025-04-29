package model.database;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "Room", schema = "dbo")
public class Room {
    @Id
    @Column(name = "RoomID", nullable = false, length = 4)
    private String roomID;

    @OneToMany(mappedBy = "roomID")
    private Set<Exam> exams = new LinkedHashSet<>();

    @OneToMany(mappedBy = "roomID")
    private Set<Slot> slots = new LinkedHashSet<>();

}