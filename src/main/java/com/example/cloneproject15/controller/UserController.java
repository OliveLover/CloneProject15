package com.example.cloneproject15.controller;

import com.example.cloneproject15.dto.ResponseDto;
import com.example.cloneproject15.dto.StatusResponseDto;
import com.example.cloneproject15.dto.UserRequestDto;
import com.example.cloneproject15.dto.UserResponseDto;
import com.example.cloneproject15.security.UserDetailsImpl;
import com.example.cloneproject15.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "UserController", description = "유저 관련 Controller")
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    @Operation(summary = "유저 가입 API" , description = "새로운 유저 가입")
    @ApiResponses(value ={@ApiResponse(responseCode= "200", description = "회원 가입 완료" )})
    @PostMapping("/signup")
    public StatusResponseDto signup(@Valid UserRequestDto requestDto,
                                    @RequestParam(value = "image", required = false) MultipartFile image) throws IOException {
        return userService.signup(requestDto, image);
    }

    @Operation(summary = "유저 로그인 API" , description = "로그인, RefreshToken, AccessToken")
    @ApiResponses(value ={@ApiResponse(responseCode= "200", description = "로그인 성공!" )})
    @PostMapping("/login")
    public StatusResponseDto login(@RequestBody UserRequestDto requestDto, HttpServletResponse response){
        return userService.login(requestDto, response);
    }

    @Operation(summary = "유저 로그아웃 API" , description = "로그아웃, RefreshToken, AccessToken")
    @ApiResponses(value ={@ApiResponse(responseCode= "200", description = "로그아웃 성공!" )})
    @PostMapping("/logout")
    public StatusResponseDto logout(@AuthenticationPrincipal UserDetailsImpl userDetails){
        return userService.logout(userDetails.getUser());
    }

    @Operation(summary = "유저 목록 API" , description = "유저목록조회, AccessToken")
    @ApiResponses(value ={@ApiResponse(responseCode= "200", description = "유저목록 조회 반환 성공!" )})
    @GetMapping("/user-info")
    public List<UserResponseDto> getUsers(@AuthenticationPrincipal UserDetailsImpl userDetails){
        String userid = userDetails.getUsername();
        return userService.getUsers(userid);
    }

    @Operation(summary = "특정 유저 정보조회 API" , description = "유저정보조회, AccessToken")
    @ApiResponses(value ={@ApiResponse(responseCode= "200", description = "유저정보 조회 반환 성공!" )})
    @GetMapping("/user-info/{userId}")
    public UserResponseDto getUserInfo(@PathVariable String userId, @AuthenticationPrincipal UserDetailsImpl userDetails){
        return userService.findUserInfo(userId);
    }

    @Operation(summary = "마이페이지 API" , description = "마이페이지 조회, AccessToken")
    @ApiResponses(value ={@ApiResponse(responseCode= "200", description = "마이페이지 반환 성공!" )})
    @GetMapping("/mypage")
    public ResponseEntity<UserResponseDto> myPage(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        return userService.myPage(userDetails.getUser());
    }

    @Operation(summary = "마이페이지 수정 API" , description = "마이페이지 수정, AccessToken")
    @ApiResponses(value ={@ApiResponse(responseCode= "200", description = "마이페이지 수정 성공!" )})
    @PutMapping("/mypage")
    public ResponseEntity<UserResponseDto> updateMypage(@RequestBody UserRequestDto userRequestDto, @AuthenticationPrincipal UserDetailsImpl userDetails) throws IOException{
//        public ResponseEntity<UserResponseDto> updateMypage(@Valid UserRequestDto userRequestDto, @RequestParam(value = "image", required = false) MultipartFile image, @AuthenticationPrincipal UserDetailsImpl userDetails) throws IOException{
        return userService.updateMypage(userRequestDto, userDetails.getUser());
    }

//    @Operation(summary = "유저 최신 생일 목록조회 API" , description = "유저 최신 생일 목록조회, AccessToken")
//    @ApiResponses(value ={@ApiResponse(responseCode= "200", description = "유저 최신 생일 목록조회 성공!" )})
//    @GetMapping("/mypage/birthday")
//    public List<UserResponseDto> checkUserByBirthday(@AuthenticationPrincipal UserDetailsImpl userDetails){
//        return userService.checkUserByBirthday();
//    }

    @Operation(summary = "유저 아이디 중복 여부" , description = "중복 여부 확인")
    @ApiResponses(value ={@ApiResponse(responseCode= "200", description = "아이디 중복여부 확인" )})
    @GetMapping("/userCheck/{userId}")
    public ResponseDto userCheck(@Valid @PathVariable String userId) {
        return userService.userCheck(userId);
    }

}
