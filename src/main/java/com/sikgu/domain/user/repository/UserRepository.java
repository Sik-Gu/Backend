package com.sikgu.domain.user.repository;

import com.sikgu.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsBySchoolEmail(String schoolEmail);
    boolean existsByNickname(String nickname);

    Optional<User> findBySchoolEmail(String schoolEmail);
}
