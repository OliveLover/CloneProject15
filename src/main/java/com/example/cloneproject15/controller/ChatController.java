package com.example.cloneproject15.controller;


import com.example.cloneproject15.dto.*;
import com.example.cloneproject15.security.UserDetailsImpl;
import com.example.cloneproject15.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.io.IOException;
import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
@Tag(name = "ChatController", description = "채팅 관련 Controller")
public class ChatController {

    private final ChatService chatService;
    private final SimpMessagingTemplate msgOperation;

    @Operation(summary = "채팅방 생성 API" , description = "새로운 채팅방 생성")
    @ApiResponses(value ={@ApiResponse(responseCode= "200", description = "채팅방 생성 완료" )})
    @PostMapping("/chat")
    public ResponseEntity<CreateChatRoomResponseDto> createChatRoom(@RequestBody ChatRoomDto chatRoomDto, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        chatRoomDto.setHost(userDetails.getUser().getUsername());
        return chatService.createChatRoom(chatRoomDto.getRoomName(), chatRoomDto.getHost(), userDetails.getUser());
    }

    @Operation(summary = "채팅방  API" , description = "새로운 채팅방 생성")
    @ApiResponses(value ={@ApiResponse(responseCode= "200", description = "채팅방 생성 완료" )})
    @GetMapping("/chat/{roomId}")
    public ResponseEntity<EnterUserDto> findChatRoom(@PathVariable String roomId, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return chatService.findRoom(roomId, userDetails.getUser().getUsername());
    }

    @MessageMapping("/chat/enter")
    @SendTo("/sub/chat/room")
    public void enterChatRoom(@RequestBody ChatDto chatDto, SimpMessageHeaderAccessor headerAccessor) throws Exception {
        Thread.sleep(500); // simulated delay
        ChatDto newchatdto = chatService.enterChatRoom(chatDto, headerAccessor);
        msgOperation.convertAndSend("/sub/chat/room" + chatDto.getRoomId(), newchatdto);
    }

    @PostMapping("/chat/image")
    public String uploadImage(@RequestParam(value = "image", required = false) MultipartFile image, @AuthenticationPrincipal UserDetailsImpl userDetails) throws IOException {
        return chatService.uploadImage(image);
    }

    @MessageMapping("/chat/send")
    @SendTo("/sub/chat/room")
    public void sendChatRoom(ChatDto chatDto, SimpMessageHeaderAccessor headerAccessor) throws Exception {
        Thread.sleep(500); // simulated delay
        chatService.sendChatRoom(chatDto, headerAccessor);
        msgOperation.convertAndSend("/sub/chat/room" + chatDto.getRoomId(), chatDto);
    }

    @EventListener
    public void webSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        ChatDto chatDto = chatService.disconnectChatRoom(headerAccessor);
        msgOperation.convertAndSend("/sub/chat/room" + chatDto.getRoomId(), chatDto);
    }

    @GetMapping("/room")
    public ResponseEntity<List<ChatRoomDto>> showRoomList() {
        return chatService.showRoomList();
    }

}