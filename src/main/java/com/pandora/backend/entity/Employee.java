package com.pandora.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import com.pandora.backend.enums.Gender;

@Getter
@Setter
@Entity // 声明这是一个实体类（会映射到数据库表）
@Table(name = "employee") // 对应数据库表名
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 主键自增（MySQL常用）
    @Column(name = "employee_id") // 对应数据库列名
    private Integer employeeId;

    // 外键：部门ID
    @ManyToOne(fetch = FetchType.LAZY) // 多个员工对应一个部门
    @JoinColumn(name = "org_id", referencedColumnName = "org_id", nullable = true, foreignKey = @ForeignKey(name = "fk_employee_department"))
    private Department department; // 对应 Department 实体类

    @Column(name = "employee_name", nullable = false, length = 64)
    private String employeeName;

    @Enumerated(EnumType.ORDINAL) // 将 Gender 枚举存为数字（0,1,2...）
    @Column(nullable = false)
    private Gender gender;

    @Column(length = 20, nullable = false)
    private String phone;

    @Column(length = 64, nullable = false)
    private String email;

    @Column(nullable = false)
    private Byte position;

    @Column(length = 20, nullable = false)
    private String password;
}
