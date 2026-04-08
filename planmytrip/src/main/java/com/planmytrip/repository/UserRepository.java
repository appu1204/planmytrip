package com.planmytrip.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.planmytrip.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long>{
    
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
    Optional<User> findByEmail(String email);
}
