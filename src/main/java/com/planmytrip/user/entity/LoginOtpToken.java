package com.planmytrip.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "login_otp_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginOtpToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "otp", nullable = false)
    private String otp;

    @Column(name = "expiry_time", nullable = false)
    private LocalDateTime expiryTime;

    @Column(name = "used", nullable = false)
    @Builder.Default
    private Boolean used = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryTime);
    }
}
