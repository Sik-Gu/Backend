package Sikgu.com.example.Web.domain.point.repository;

import Sikgu.com.example.Web.domain.point.entity.PointHold;
import Sikgu.com.example.Web.domain.point.enums.HoldStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointHoldRepository extends JpaRepository<PointHold, Long> {

    // refund: 이 참여의 보관중 보류 1건
    Optional<PointHold> findByParticipationIdAndStatus(Long participationId, HoldStatus status);

    // settle: 이 공구의 보관중 보류 전부
    List<PointHold> findAllByPostIdAndStatus(Long postId, HoldStatus status);
}