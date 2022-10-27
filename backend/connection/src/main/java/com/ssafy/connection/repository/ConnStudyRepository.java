package com.ssafy.connection.repository;

import com.ssafy.connection.entity.ConnStudy;
import com.ssafy.connection.securityOauth.domain.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConnStudyRepository extends JpaRepository<ConnStudy, Long> {
    ConnStudy findByUser(User userEntity);
    Optional<ConnStudy> findByStudy_StudyId(long studyId);
}