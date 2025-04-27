package model.database;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "Payment", schema = "dbo")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PaymentID", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "FeeID", nullable = false)
    private Fee feeID;

    @Column(name = "PaymentDate")
    private Instant paymentDate;

    @Column(name = "AmountPaid", nullable = false)
    private BigDecimal amountPaid;

    @Column(name = "PaymentStatus", nullable = false)
    private Integer paymentStatus;

}