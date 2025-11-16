package com.pandora.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "department")
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "org_id")
    private Integer orgId;

    @Column(name = "org_name", nullable = false, length = 64)
    private String orgName;

    // 一个部门可以有多个团队
    @OneToMany(mappedBy = "department", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Team> teams;

    // 一个部门可以有多个员工
    @OneToMany(mappedBy = "department", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Employee> employees;

    /**
     * 部门经理
     * 一个部门只有一个经理。
     * @JoinColumn 指定了在 department 表中用于关联 Employee 的外键列名。
     */
    @OneToOne(fetch = FetchType.LAZY) // 使用LAZY懒加载以提高性能
    @JoinColumn(name = "manager_id", referencedColumnName = "employee_id") // 'manager_id' 是数据库中新建的外键列
    private Employee manager;


// --- 现在，您之前添加的 getter 和 setter 就能正常工作了 ---

    public Employee getManager() {
        return manager;
    }

    public void setManager(Employee manager) {
        this.manager = manager;
    }
}
