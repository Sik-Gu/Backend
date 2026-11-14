package Sikgu.com.example.Web.domain.user.repository;

import Sikgu.com.example.Web.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByLoginId(String loginId);

    boolean existsBySchoolEmail(String schoolEmail);
}
