package com.example.cloneproject15.service;

import com.example.cloneproject15.entity.User;
import com.example.cloneproject15.entity.UserRoleEnum;
import com.example.cloneproject15.repository.UserRepository;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import static com.example.cloneproject15.entity.UserRoleEnum.USER;
import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@DataJpaTest
class UserServiceTest {

    @Autowired
    UserRepository userRepository;

    @Test
    @DisplayName("username과 password가 유효하다면 회원가입이 완료된다.")
    public void testSignUpWithValidCredentials() {
        // given
        String userId = "userId1";
        String userName = "도라에몽";
        String birthday = "1988-05-07";
        String image_url = "http://image_url1";
        String comment = "comment1";
        String password = "qwerasdfzxcv";

        User user = new User(userId, password, userName, USER, image_url, birthday, comment);

        // when
        userRepository.save(user);

        // then
        assertEquals(userId, user.getUserid());
        assertEquals(userName, user.getUsername());
        assertEquals(birthday, user.getBirthday());
        assertEquals(image_url, user.getProfile_image());
        assertEquals(comment, user.getComment());
        assertEquals(password, user.getPassword());
    }

    @Test
    @DisplayName("이름이 한글이 아니라면 ConstrainViolationException 예외가 발생한다.")
    public void testSignUpWithNonKoreanName() {
        // given
        String userId = "userId1";
        String userName = "doraemon";
        String birthday = "1988-05-07";
        String image_url = "http://image_url1";
        String comment = "comment1";
        String password = "qwerasdfzxcv";

        User user = new User(userId, password, userName, USER, image_url, birthday, comment);

        // when & then
        assertThrows(ConstraintViolationException.class, () -> {
            userRepository.save(user);});
    }

    @Test
    @DisplayName("password가 null 이라면 DataIntegrityViolationException 예외가 발생한다.")
    public void testSignUpWithNullPassword() {
        // given
        String userId = "userId1";
        String userName = "도라에몽";
        String birthday = "1988-05-07";
        String image_url = "http://image_url1";
        String comment = "comment1";
        String password = null;

        User user = new User(userId, password, userName, USER, image_url, birthday, comment);

        // when & then
        assertThrows(DataIntegrityViolationException.class, () -> {
            userRepository.save(user);});
    }


}