package com.example.cloneproject15.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest
class ChatServiceTest {

    @Test
    @DisplayName("채팅방 한 개 생성")
    public void createChatRoom() {
        //given
        String roomName = "Room1";

        //when

        //then

    }

}