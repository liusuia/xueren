package com.xueren.repository;

import com.xueren.entity.Moment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MomentRepository extends JpaRepository<Moment, Long> {
    List<Moment> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Moment> findByUserIdInOrderByCreatedAtDesc(List<Long> userIds);
}
