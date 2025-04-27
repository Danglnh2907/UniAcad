package model.database;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

@Getter
@Setter
@Entity
@Table(name = "Staff", schema = "dbo")
public class Staff {
    @Id
    @Column(name = "StaffID", nullable = false, length = 20)
    private String staffID;

    @Column(name = "StaffName", nullable = false, length = 100)
    private String staffName;

    @Column(name = "StaffEmail", nullable = false, length = 50)
    private String staffEmail;

    @Column(name = "StaffPhone", nullable = false, length = 20)
    private String staffPhone;

    @ColumnDefault("0")
    @Column(name = "StaffStatus")
    private Integer staffStatus;

}