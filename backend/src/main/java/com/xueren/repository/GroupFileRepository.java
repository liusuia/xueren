package com.xueren.repository;

import com.xueren.entity.GroupFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupFileRepository extends JpaRepository<GroupFile, Long> {

    List<GroupFile> findByGroupIdOrderByCreatedAtDesc(Long groupId);

    long countByGroupId(Long groupId);
}
