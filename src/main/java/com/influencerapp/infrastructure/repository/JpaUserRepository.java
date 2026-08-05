package com.influencerapp.infrastructure.repository;

import com.influencerapp.infrastructure.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
/**
 * Spring Data JPA repository interface for user entities.
 *
 * @author judaro122
 * @since 1.0.0
 */



public interface JpaUserRepository extends JpaRepository<UserEntity, String> {

    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findById(String id);
}
