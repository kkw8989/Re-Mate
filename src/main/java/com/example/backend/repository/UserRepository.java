package com.example.backend.repository;

import com.example.backend.domain.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

// JpaRepository를 상속받아 기본 SQL(저장, 조회 등) 기능을 구현 없이 사용함
public interface UserRepository extends JpaRepository<User, Long> {

  // 구글 로그인을 시도한 이메일이 이미 DB에 있는지 확인하는 기능
  // Optional: 데이터가 없을 경우(null)를 안전하게 처리함
  Optional<User> findByEmail(String email);
}
