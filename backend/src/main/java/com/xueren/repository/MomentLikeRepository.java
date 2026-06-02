package com.xueren.repository;

import com.xueren.entity.MomentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MomentLikeRepository extends JpaRepository<MomentLike, Long> {
    List<MomentLike> findByMomentId(Long momentId);
    boolean existsByMomentIdAndUserId(Long momentId, Long userId);
    void deleteByMomentIdAndUserId(Long momentId, Long userId);
}
