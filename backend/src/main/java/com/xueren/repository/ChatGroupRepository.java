package com.xueren.repository;

import com.xueren.entity.ChatGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatGroupRepository extends JpaRepository<ChatGroup, Long> {

    @Query("SELECT g FROM ChatGroup g WHERE g.name LIKE %:keyword%")
    List<ChatGroup> searchByName(@Param("keyword") String keyword);
}
