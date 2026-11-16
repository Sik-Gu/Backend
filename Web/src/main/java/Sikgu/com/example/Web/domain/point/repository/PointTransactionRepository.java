package Sikgu.com.example.Web.domain.point.repository;

import Sikgu.com.example.Web.domain.point.entity.PointTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointTransactionRepository extends JpaRepository<PointTransaction, Long> {
}