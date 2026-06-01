package com.xueren.repository;

import com.xueren.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    List<Favorite> findByUserIdOrderByCreatedAtDesc(Long userId);
    void deleteByUserIdAndMessageId(Long userId, Long messageId);
    boolean existsByUserIdAndMessageId(Long userId, Long messageId);
}
