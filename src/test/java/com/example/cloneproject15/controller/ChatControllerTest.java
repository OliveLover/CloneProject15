package com.example.cloneproject15.controller;

import com.example.cloneproject15.dto.ChatRoomDto;
import com.example.cloneproject15.dto.CreateChatRoomResponseDto;
import com.example.cloneproject15.dto.ResponseDto;
import com.example.cloneproject15.entity.ChatRoom;
import com.example.cloneproject15.entity.User;
import com.example.cloneproject15.security.UserDetailsImpl;
import com.example.cloneproject15.service.ChatService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static com.example.cloneproject15.entity.UserRoleEnum.USER;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class ChatControllerTest {

    @MockBean
    ChatService chatService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    @DisplayName("createChatRoom 메서드를 호출하면 JSON형식의 응답이 반환된다.")
    void createChatRoom() throws Exception {
        // given
        User user = new User("userId1", "qwerasdf", "도라에몽", USER, "http://profile-image", "1995-05-08", "comment1");
        UserDetailsImpl userDetails = new UserDetailsImpl(user, user.getUsername());
        ChatRoomDto chatRoomDto = new ChatRoomDto(null, "Room1", user.getUsername(), 0L, user.getProfile_image());
        ChatRoom newChatRoom = new ChatRoom(chatRoomDto.getRoomName(), chatRoomDto.getHost(), userDetails.getUser().getUserid());
        CreateChatRoomResponseDto responseDto = new CreateChatRoomResponseDto("create ChatRoom success", newChatRoom.getRoomId());

        given(chatService.createChatRoom(anyString(), anyString(), any(User.class)))
                .willReturn(new ResponseEntity<>(responseDto, HttpStatus.CREATED));

        // when & then
        mockMvc.perform(post("/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(chatRoomDto))
                        .with(user(userDetails)))
                .andExpect(jsonPath("$.msg").exists())
                .andExpect(jsonPath("$.roomId").exists())
                .andExpect(status().isCreated())
                .andDo(print());

    }
}