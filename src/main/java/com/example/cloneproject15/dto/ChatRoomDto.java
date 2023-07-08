package com.example.cloneproject15.dto;

import com.example.cloneproject15.entity.ChatRoom;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomDto {
    private String roomId;
    private String roomName;
    private String host;
    private Long headCount;
    private String profile_image;

    public ChatRoomDto(ChatRoom chatRoom, String profile_image) {
        this.roomId = chatRoom.getRoomId();
        this.roomName = chatRoom.getRoomName();
        this.host = chatRoom.getHost();
        this.profile_image = profile_image;
    }

}
