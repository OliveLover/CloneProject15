package com.example.cloneproject15.service;

import com.example.cloneproject15.entity.ChatRoom;
import com.example.cloneproject15.entity.User;
import com.example.cloneproject15.repository.ChatRoomRepository;
import com.example.cloneproject15.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static com.example.cloneproject15.entity.UserRoleEnum.USER;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@DataJpaTest
class ChatServiceTest {

    @Autowired
    ChatRoomRepository chatRoomRepository;

    @Autowired
    UserRepository userRepository;

    @Test
    @DisplayName("채팅방 2개 생성하고 조회한다.")
    public void findAllByChatRoom() {
        // given
        ChatRoom chatRoom1 = new ChatRoom("Room1", "user1", "userId1");
        ChatRoom chatRoom2 = new ChatRoom("Room2", "user2", "userId2");

        chatRoomRepository.saveAll(List.of(chatRoom1, chatRoom2));

        // when
        List<ChatRoom> chatRooms =chatRoomRepository.findAll();

        // then
        assertThat(chatRooms).hasSize(2);
        assertThat(chatRooms.get(0))
                .isEqualTo(chatRoom1)
                .extracting(ChatRoom::getRoomName, ChatRoom::getHost, ChatRoom::getUserid)
                .containsExactly("Room1", "user1", "userId1");

        assertThat(chatRooms.get(1))
                .isEqualTo(chatRoom2)
                .extracting(ChatRoom::getRoomName, ChatRoom::getHost, ChatRoom::getUserid)
                .containsExactly("Room2", "user2", "userId2");

    }

    @Test
    @DisplayName("채팅방을 입장하면 인원 수가 3명 오른다.")
    public void countUpWhenEnterTheRoom() {
        // given
        ChatRoom chatRoom = new ChatRoom("Room1", "user1", "userId1");
        List<ChatRoom> chatRooms = chatRoomRepository.saveAll(List.of(chatRoom));
        assertThat(chatRooms).hasSize(1);

        User user1 = new User("userId1", "passWord", "김유저", USER, null, "1995-09-08", "comment1");
        User user2 = new User("userId2", "passWord", "이유저", USER, null, "1992-12-03", "comment2");
        User user3 = new User("userId3", "passWord", "박유저", USER, null, "1999-03-27", "comment3");
        List<User> users = userRepository.saveAll(List.of(user1, user2, user3));
        assertThat(users).hasSize(3);

        user1.enterRoom(chatRoom);
        user2.enterRoom(chatRoom);
        user3.enterRoom(chatRoom);

        Long headCount = userRepository.countAllByRoom_Id(chatRoom.getId());

        // when
        chatRoom.updateCount(headCount);

        // then
        assertThat(chatRoom.getHeadCount()).isEqualTo(3);

    }

    @Test
    @DisplayName("채팅방을 입장하면 인원 수가 2명 이었다가 0명이 되면 채팅방은 사라진다.")
    public void chatRoomDisappearsWhenParticipantsBecomeZero() {
        // given
        ChatRoom chatRoom = new ChatRoom("Room1", "user1", "userId1");
        List<ChatRoom> chatRooms = chatRoomRepository.saveAll(List.of(chatRoom));
        assertThat(chatRooms).hasSize(1);

        User user1 = new User("userId1", "passWord", "김유저", USER, null, "1995-09-08", "comment1");
        User user2 = new User("userId2", "passWord", "이유저", USER, null, "1992-12-03", "comment2");
        List<User> users = userRepository.saveAll(List.of(user1, user2));
        assertThat(users).hasSize(2);

        user1.enterRoom(chatRoom);
        user2.enterRoom(chatRoom);

        Long beforeHeadCount = userRepository.countAllByRoom_Id(chatRoom.getId());
        chatRoom.updateCount(beforeHeadCount);
        assertThat(chatRoom.getHeadCount()).isEqualTo(2);

        user1.exitRoom(chatRoom);
        user2.exitRoom(chatRoom);
        Long afterHeadCount = userRepository.countAllByRoom_Id(chatRoom.getId());

        assertThat(afterHeadCount).isZero();

        // when
        if(afterHeadCount == 0) chatRoomRepository.deleteByRoomId(chatRoom.getRoomId());

        // then
        chatRooms = chatRoomRepository.findAll();
        assertThat(chatRooms).hasSize(0);

    }

}