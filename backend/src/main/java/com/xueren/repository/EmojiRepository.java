package com.xueren.repository;

import com.xueren.entity.Emoji;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EmojiRepository extends JpaRepository<Emoji, Long> {
    List<Emoji> findByUserIdOrderByCreatedAtDesc(Long userId);
    void deleteByUserIdAndId(Long userId, Long id);
    boolean existsByUserIdAndId(Long userId, Long id);
    boolean existsByUserIdAndFileHash(Long userId, String fileHash);
}
