package com.xueren.repository;

import com.xueren.entity.MomentComment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MomentCommentRepository extends JpaRepository<MomentComment, Long> {
    List<MomentComment> findByMomentIdOrderByCreatedAtAsc(Long momentId);
}
