package Sikgu.com.example.Web.domain.report.repository;

import Sikgu.com.example.Web.domain.report.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {
}
