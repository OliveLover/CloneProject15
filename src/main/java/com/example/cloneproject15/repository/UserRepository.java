package com.example.cloneproject15.repository;

import com.example.cloneproject15.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUserid(String userid);
    Optional<User> findByUsername(String username);
    Optional<User> findByKakaoid(Long id);
    Optional<User> findByEmail(String email);
    List<User> findAllByOrderByUsernameDesc();

//    @Query("SELECT u FROM TB_USERS u WHERE str_to_date(concat(Year(Date(NOW())),substr(u.birthday, 5)), '%Y-%m-%d') between CURDATE() and CURDATE()+5")
//    List<User> findByUserAndBirthday();

    Long countAllByRoom_Id(Long roomId);

  }

