package com.example.cloneproject15.entity;

import com.example.cloneproject15.dto.UserRequestDto;
import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity(name = "TB_USERS")
@Getter
@NoArgsConstructor
@Table
public class User extends TimeEntity{

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long Id;

    @Pattern(regexp = "[a-zA-Z0-9]{4,12}$", message="아이디는 알파벳 대문자, 소문자, 숫자 포함 4~12자리여야 합니다.")
    @Column(nullable = false, unique = true)
    private String userid;

    @Column(nullable = false)
    private String password;

    @Pattern(regexp = "[가-힣]{2,10}$", message="이름은 한글 2~10자리여야 합니다.")
    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    @Enumerated(value = EnumType.STRING)
    private UserRoleEnum role;

    @Column(nullable = true)
    private Long kakaoid;

    @Column(nullable = true)
    private String email;

    @Column(nullable = true)
    private String profile_image;

    @Column(nullable = false)
    private String birthday;

    @Column(nullable = true)
    private String comment;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Chat> chatList = new ArrayList<>();

    @ManyToOne
    private ChatRoom room;


    public User(String userid, String password, String username, UserRoleEnum role, String profile_image, String birthday,String comment) {
        this.userid = userid;
        this.password = password;
        this.username = username;
        this.role = role;
        this.profile_image = profile_image;
        this.birthday = birthday;
        this.comment = comment;
    }

    public User(String userid, String password, String username, UserRoleEnum role, Long kakaoid, String email, String profile_image, String birthday) {
        this.userid = userid;
        this.password = password;
        this.username = username;
        this.role = role;
        this.kakaoid = kakaoid;
        this.email = email;
        this.profile_image = profile_image;
        this.birthday = birthday;
    }

    public void update(UserRequestDto userRequestDto){
        this.password = userRequestDto.getPassword();
        this.username = userRequestDto.getUsername();
        this.birthday = userRequestDto.getBirthday();
        this.comment = userRequestDto.getComment();
    }


    public User kakaoIdUpdate(Long kakaoid) {
        this.kakaoid = kakaoid;
        return this;
    }

    public void enterRoom(ChatRoom room) {
        this.room = room;
    }

    public void exitRoom(ChatRoom room) {
        this.room = null;
    }
}
