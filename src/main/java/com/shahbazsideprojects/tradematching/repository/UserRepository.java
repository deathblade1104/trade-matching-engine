package com.shahbazsideprojects.tradematching.repository;

import com.shahbazsideprojects.tradematching.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
}
