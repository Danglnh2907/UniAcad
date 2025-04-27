package model.database;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "Fee", schema = "dbo")
public class Fee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "FeeID", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "StudentID", nullable = false)
    private Student studentID;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TermID")
    private Term termID;

    @Column(name = "Amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "DueDate")
    private Instant dueDate;

    @Column(name = "FeeStatus", nullable = false)
    private Integer feeStatus;

    @OneToMany(mappedBy = "feeID")
    private Set<Payment> payments = new LinkedHashSet<>();

}