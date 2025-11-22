package com.w_s_backend.w_s.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.w_s_backend.w_s.models.User;

public interface UserRepository extends JpaRepository<User, Long>{

}
