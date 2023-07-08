package com.example.cloneproject15.entity;

import com.example.cloneproject15.dto.ChatDto;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity(name = "TB_CHAT")
@Getter
@NoArgsConstructor
public class Chat extends TimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_Id")
    private ChatRoom room;

    private String sender;

    private String message;

    private String userid;

    @Enumerated(EnumType.STRING)
    private MessageType type;

    @Column(nullable = true)
    private String profile_image;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    public Chat (String sender, String message, ChatRoom room, User user, MessageType type, String profile_image) {
        this.sender = sender;
        this.message = message;
        this.room = room;
        this.user = user;
        this.userid = user.getUserid();
        this.type = type;
        this.profile_image = profile_image;
    }



}
