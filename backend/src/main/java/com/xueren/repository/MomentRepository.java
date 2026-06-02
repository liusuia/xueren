package com.xueren.repository;

import com.xueren.entity.Moment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MomentRepository extends JpaRepository<Moment, Long> {
    List<Moment> findByUserIdOrderByCreatedAtDesc(Long userId);
    @org.springframework.data.jpa.repository.Query("SELECT m FROM Moment m WHERE m.userId IN :userIds ORDER BY m.createdAt DESC")
    List<Moment> findByUserIdInOrderByCreatedAtDesc(@org.springframework.data.repository.query.Param("userIds") List<Long> userIds);
}
