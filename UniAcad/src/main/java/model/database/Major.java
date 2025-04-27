package model.database;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "Major", schema = "dbo")
public class Major {
    @Id
    @Column(name = "MajorID", nullable = false, length = 2)
    private String majorID;

    @Column(name = "MajorName", nullable = false, length = 100)
    private String majorName;

    @OneToMany(mappedBy = "majorID")
    private Set<Curriculum> curricula = new LinkedHashSet<>();

}