package com.example.cloneproject15.controller;

import com.example.cloneproject15.dto.*;
import com.example.cloneproject15.entity.Chat;
import com.example.cloneproject15.entity.ChatRoom;
import com.example.cloneproject15.entity.User;
import com.example.cloneproject15.repository.ChatRepository;
import com.example.cloneproject15.repository.ChatRoomRepository;
import com.example.cloneproject15.repository.UserRepository;
import com.example.cloneproject15.security.UserDetailsImpl;
import com.example.cloneproject15.service.ChatService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.InputStreamSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.converter.SimpleMessageConverter;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static com.example.cloneproject15.entity.MessageType.ENTER;
import static com.example.cloneproject15.entity.MessageType.TALK;
import static com.example.cloneproject15.entity.UserRoleEnum.USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class ChatControllerTest {
    @Autowired
    ChatRepository chatRepository;

    @Autowired
    ChatRoomRepository chatRoomRepository;

    @Autowired
    UserRepository userRepository;

    @MockBean
    ChatService chatService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

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
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.msg").exists())
                .andExpect(jsonPath("$.roomId").exists())
                .andDo(print());

    }

    @Test
    @Transactional
    @DisplayName("findChatRoom 메서드를 호출하면 JSON형식의 응답이 반환된다.")
    void findChatRoom() throws Exception {
        // given
        User user = new User("userId1", "qwerasdf", "도라에몽", USER, "http://profile-image", "1995-05-08", "comment1");
        UserDetailsImpl userDetails = new UserDetailsImpl(user, user.getUsername());
        userRepository.save(user);

        ChatRoomDto chatRoomDto = new ChatRoomDto(null, "Room1", user.getUsername(), 0L, user.getProfile_image());
        ChatRoom findChatRoom = new ChatRoom(chatRoomDto.getRoomName(), chatRoomDto.getHost(), userDetails.getUser().getUserid());
        List<ChatRoom> chatRooms = chatRoomRepository.saveAll(List.of(findChatRoom));


        List<ChatDto> chatDtoList = new ArrayList<>();

        EnterUserDto responseDto = new EnterUserDto(user.getUsername(), user.getUserid(), findChatRoom.getRoomId(), user.getProfile_image(),chatDtoList);

        given(chatService.findRoom(anyString(), anyString()))
                .willReturn(new ResponseEntity<>(responseDto, HttpStatus.OK));

        // when & then
        mockMvc.perform(get("/chat/{roomId}", chatRooms.get(0).getRoomId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(chatRoomDto))
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sender").exists())
                .andExpect(jsonPath("$.userId").exists())
                .andExpect(jsonPath("$.roomId").value(chatRooms.get(0).getRoomId()))
                .andExpect(jsonPath("$.profile_image").exists())
                .andDo(print());

    }

    @Test
    @Transactional
    @DisplayName("uploadImage 메서드를 호출하면 JSON형식의 응답이 반환된다.")
    void uploadImage() throws Exception {
        // given
        User user = new User("userId1", "qwerasdf", "도라에몽", USER, "http://profile-image", "1995-05-08", "comment1");
        UserDetailsImpl userDetails = new UserDetailsImpl(user, user.getUsername());
        userRepository.save(user);

        MockMultipartFile image = new MockMultipartFile(
                "image",
                "image.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "image data".getBytes()
        );

        given(chatService.uploadImage(any()))
                .willReturn("http://uploaded-image-url");

        // when & then
        mockMvc.perform(multipart("/chat/image")
                        .file(image)
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(content().string("http://uploaded-image-url"))
                .andDo(print());

    }

    @Test
    @Transactional
    @DisplayName("showRoomList 메서드를 호출하면 JSON형식의 응답이 반환된다.")
    void showRoomList() throws Exception {
        // given
        List<ChatRoomDto> chatRoomDtoList = new ArrayList<>();
        given(chatService.showRoomList())
                .willReturn(new ResponseEntity<>(chatRoomDtoList, HttpStatus.OK));

        // when & then
        mockMvc.perform(get("/room")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(chatRoomDtoList)))
                .andExpect(status().isOk())
                .andDo(print());

    }

    @Test
    @DisplayName("enterChatRoom 메서드가 웹소켓 통신을 통해 메시지를 정상적으로 처리한다.")
    void enterChatRoom() throws Exception {
        // given
        ChatDto chatDto = new ChatDto(ENTER, "도라에몽", "userId1", "roomId1", "2023-07-08", "입장!", "http://profile_image", "image_url");
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create();

        given(chatService.enterChatRoom(any(), any())).willReturn(chatDto);

        // when
        messagingTemplate.convertAndSend("/sub/chat/room", chatDto);

        // then
        ArgumentCaptor<ChatDto> chatDtoCaptor = ArgumentCaptor.forClass(ChatDto.class);
        verify(messagingTemplate, times(1)).convertAndSend(eq("/sub/chat/room"), chatDtoCaptor.capture());
        ChatDto sentChatDto = chatDtoCaptor.getValue();
        assertEquals("도라에몽", sentChatDto.getSender());
        assertEquals("userId1", sentChatDto.getUserId());
    }
}