package com.planmytrip.user.repository;

import com.planmytrip.user.entity.LoginOtpToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LoginOtpTokenRepository extends JpaRepository<LoginOtpToken, Long> {

    Optional<LoginOtpToken> findTopByUserIdAndUsedFalseOrderByCreatedAtDesc(Long userId);

    void deleteAllByUserId(Long userId);
}
