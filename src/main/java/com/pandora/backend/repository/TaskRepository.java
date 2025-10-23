package com.pandora.backend.repository;

import com.pandora.backend.entity.Task; // 确保导入了你的 Task 实体
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository // 告诉 Spring 这是一个数据仓库组件
public interface TaskRepository extends JpaRepository<Task, Integer> {
    // JpaRepository<Task, Integer> 中的 Task 是实体类名，Integer 是主键的类型
    // 请确保 Task 实体的主键 taskId 的类型是 Integer

    // 目前不需要在这里添加任何自定义方法，继承 JpaRepository 就已经拥有了
    // findById(), findAll(), save(), deleteById() 等所有基础的 CRUD 功能。
}