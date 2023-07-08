package com.example.cloneproject15.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.example.cloneproject15.dto.*;
import com.example.cloneproject15.entity.Chat;
import com.example.cloneproject15.entity.ChatRoom;
import com.example.cloneproject15.entity.MessageType;
import com.example.cloneproject15.entity.User;
import com.example.cloneproject15.exception.ApiException;
import com.example.cloneproject15.exception.ExceptionEnum;
import com.example.cloneproject15.repository.ChatRepository;
import com.example.cloneproject15.repository.ChatRoomRepository;
import com.example.cloneproject15.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.temporal.ChronoField;
import java.util.*;


@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ChatService {
    private static final String S3_BUCKET_PREFIX = "S3";

    @Value("chattingroom")
    private String bucketName;
    private final AmazonS3 amazonS3;

    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final ChatRepository chatRepository;

    public ResponseEntity<CreateChatRoomResponseDto> createChatRoom(String roomName, String host, User user) {
        Optional<ChatRoom> findChatRoom = validExistChatRoom(host, roomName);
        if(findChatRoom.isPresent()) {
            CreateChatRoomResponseDto responseDto = new CreateChatRoomResponseDto("already has room and find Chatting Room Success!", findChatRoom.get().getRoomId());
            return new ResponseEntity<>(responseDto, HttpStatus.NOT_ACCEPTABLE);
        }

        ChatRoom newChatRoom = new ChatRoom(roomName, host, user.getUserid());
        chatRoomRepository.save(newChatRoom);
        CreateChatRoomResponseDto responseDto = new CreateChatRoomResponseDto("create ChatRoom success", newChatRoom.getRoomId());
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    public ResponseEntity<EnterUserDto> findRoom(String roomId, String userName) {
        ChatRoom chatRoom = roomIdCheck(roomId);
        User user = userNameCheck(userName);
        List<Chat> chatList = chatRepository.findAllByRoom_IdOrderByCreatedDateAsc(chatRoom.getId());
        List<ChatDto> chatDtoList = new ArrayList<>();
        for (Chat chat : chatList) {
            ChatDto chatDto = new ChatDto(chat);
            chatDtoList.add(chatDto);
        }
        EnterUserDto enterUserDto = new EnterUserDto(userName, user.getUserid(), chatRoom.getRoomId(), user.getProfile_image(), chatDtoList);
        return new ResponseEntity<>(enterUserDto, HttpStatus.OK);
    }

    public ChatDto enterChatRoom(ChatDto chatDto, SimpMessageHeaderAccessor headerAccessor) {
        ChatRoom chatRoom = validExistChatRoom(chatDto.getRoomId());
        headerAccessor.getSessionAttributes().put("userId", chatDto.getUserId());
        headerAccessor.getSessionAttributes().put("roomId", chatDto.getRoomId());
        headerAccessor.getSessionAttributes().put("nickName", chatDto.getSender());

        User user = userIDCheck(chatDto.getUserId());
        ChatRoom room = roomIdCheck(chatDto.getRoomId());
        user.enterRoom(room);

        chatDto.setMessage(chatDto.getSender() + "님 입장!! ο(=•ω＜=)ρ⌒☆");

        Long headCount = userRepository.countAllByRoom_Id(chatRoom.getId());
        chatRoom.updateCount(headCount);
        return chatDto;
    }

    public ChatDto disconnectChatRoom(SimpMessageHeaderAccessor headerAccessor) {
        String roomId = (String) headerAccessor.getSessionAttributes().get("roomId");
        String nickName = (String) headerAccessor.getSessionAttributes().get("nickName");
        String userId = (String) headerAccessor.getSessionAttributes().get("userId");
        User user = userNameCheck(nickName);
        ChatRoom room = roomIdCheck(roomId);
        user.exitRoom(room);

        ChatDto chatDto = ChatDto.builder()
                .type(MessageType.LEAVE)
                .userId(userId)
                .roomId(roomId)
                .sender(nickName)
                .userId(userId)
                .message(nickName + "님 퇴장!! ヽ(*。>Д<)o゜")
                .build();

        Long headCount = userRepository.countAllByRoom_Id(room.getId());
        room.updateCount(headCount);
        if(headCount == 0){
            chatRoomRepository.deleteByRoomId(roomId);
        }

        return chatDto;
    }

    public Optional<ChatRoom> validExistChatRoom(String host, String roomName) {
        return chatRoomRepository.findByHostAndRoomName(host, roomName);
    }

    public ChatRoom validExistChatRoom(String roomId) {
        return chatRoomRepository.findByRoomId(roomId).orElseThrow(
                ()-> new NoSuchElementException("채팅방이 존재하지 않습니다.")
        );
    }

    public ResponseEntity<List<ChatRoomDto>> showRoomList() {
        List<ChatRoom>chatRoomList = chatRoomRepository.findAll();
        List<ChatRoomDto> chatRoomDtoList = new ArrayList<>();
        for (ChatRoom chatRoom : chatRoomList) {
            Optional<User> findUser = userRepository.findByUserid(chatRoom.getUserid());
            String profile_image = findUser.get().getProfile_image();
            ChatRoomDto chatRoomDto = new ChatRoomDto(chatRoom, profile_image);
            chatRoomDtoList.add(chatRoomDto);
        }
        return new ResponseEntity<>(chatRoomDtoList, HttpStatus.OK);
    }

    public ChatRoom roomIdCheck(String roomId) {
        return chatRoomRepository.findByRoomId(roomId).orElseThrow(
                () -> new IllegalArgumentException("존재하지 않는 채팅방입니다.")
        );
    }

    public User userNameCheck(String userName) {
        return userRepository.findByUsername(userName).orElseThrow(
                () -> new IllegalArgumentException("존재하지 않는 유저입니다.")
        );
    }

    public User userIDCheck(String userId) {
        return userRepository.findByUserid(userId).orElseThrow(
                () -> new IllegalArgumentException("존재하지 않는 유저입니다.")
        );
    }

    public void sendChatRoom(ChatDto chatDto, SimpMessageHeaderAccessor headerAccessor) {

        ChatRoom room = roomIdCheck(chatDto.getRoomId());
        User user = userIDCheck(chatDto.getUserId());
        String profile_image = chatDto.getProfile_image();
        MessageType type = MessageType.TALK;

        Date date = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateformat = format.format(date);
        chatDto.setDate(dateformat);
        chatDto.setProfile_image(user.getProfile_image());

        Chat chat = new Chat(chatDto.getSender(),chatDto.getMessage(), room, user, type, profile_image);
        chatRepository.save(chat);
    }

    public String uploadImage(MultipartFile image) throws IOException {
        String image_url = "이미지 업로드 실패";

        LocalDateTime now = LocalDateTime.now();
        int hour = now.getHour();
        int minute = now.getMinute();
        int second = now.getSecond();
        int millis = now.get(ChronoField.MILLI_OF_SECOND);

        if(image != null){
            String newFileName = "image"+hour+minute+second+millis;
            String fileExtension = '.'+image.getOriginalFilename().replaceAll("^.*\\.(.*)$", "$1");
            String imageName = S3_BUCKET_PREFIX + newFileName + fileExtension;

            String[] extensionArray = {".png", ".jpeg", ".jpg", ".webp", ".gif"};

            List<String> extensionList = new ArrayList<>(Arrays.asList(extensionArray));

            if(!extensionList.contains(fileExtension)){
                throw new ApiException(ExceptionEnum.UNAUTHORIZED_FILE);
            }

            if(image.getSize() > 20971520){
                throw new ApiException(ExceptionEnum.MAX_FILE_SIZE);
            }

            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentType(image.getContentType());
            objectMetadata.setContentLength(image.getSize());

            InputStream inputStream = image.getInputStream();

            amazonS3.putObject(new PutObjectRequest(bucketName, imageName, inputStream, objectMetadata)
                    .withCannedAcl(CannedAccessControlList.PublicRead));
            image_url = amazonS3.getUrl(bucketName, imageName).toString();
        }
        return image_url;
    }
}