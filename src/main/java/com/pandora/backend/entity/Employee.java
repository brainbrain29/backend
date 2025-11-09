package com.pandora.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.pandora.backend.enums.Gender;
import java.util.List;

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

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "gender", nullable = false)
    private Gender gender;

    @Column(name = "email", nullable = false, length = 64)
    private String email;

    @Column(name = "position", nullable = false)
    private Byte position;

    @JsonIgnore
    @Column(name = "emp_password", nullable = false, length = 20)
    private String password;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "avatar_url", length = 255)
    private String avatarUrl;

    @OneToMany(mappedBy = "employee")
    private List<Employee_Team> employeeTeams;

    @OneToMany(mappedBy = "employee", fetch = FetchType.LAZY)
    @JsonManagedReference(value = "emp-logs")
    private List<Log> logs;
}
