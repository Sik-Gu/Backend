package com.sikgu.web.domain.post.repository;

import com.sikgu.web.domain.post.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {
}