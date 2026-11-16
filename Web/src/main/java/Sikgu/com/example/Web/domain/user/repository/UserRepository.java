package Sikgu.com.example.Web.domain.user.repository;

import Sikgu.com.example.Web.domain.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findBySchoolEmail(String schoolEmail);

    boolean existsBySchoolEmail(String schoolEmail);
}
