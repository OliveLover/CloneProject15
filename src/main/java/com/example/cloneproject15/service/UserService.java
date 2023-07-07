package com.example.cloneproject15.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.example.cloneproject15.config.SentrySupport;
import com.example.cloneproject15.dto.*;
import com.example.cloneproject15.entity.RefreshToken;
import com.example.cloneproject15.entity.User;
import com.example.cloneproject15.entity.UserRoleEnum;
import com.example.cloneproject15.exception.ApiException;
import com.example.cloneproject15.exception.ExceptionEnum;
import com.example.cloneproject15.jwt.JwtUtil;
import com.example.cloneproject15.repository.RefreshTokenRepository;
import com.example.cloneproject15.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.temporal.ChronoField;
import java.util.*;
import java.util.stream.Collectors;

import static com.example.cloneproject15.dto.StatusCode.BAD_REQUEST;
import static com.example.cloneproject15.dto.StatusCode.OK;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final SentrySupport sentrySupport;
    private static final String S3_BUCKET_PREFIX = "S3";

    @Value("chattingroom")
    private String bucketName;
    private final AmazonS3 amazonS3;

    public StatusResponseDto signup(UserRequestDto requestDto, MultipartFile image) throws IOException {

        String userid = requestDto.getUserid();
        String username = requestDto.getUsername();
        String birthday = requestDto.getBirthday();

        if (!requestDto.getPassword().matches("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{6,}$")) { // 비밀번호 정규식 체크
            sentrySupport.logSimpleMessage((ExceptionEnum.PASSWAORD_REGEX).getMessage());
            throw new ApiException(ExceptionEnum.PASSWAORD_REGEX);
        }

        String password = passwordEncoder.encode(requestDto.getPassword());

        Optional<User> findUser = userRepository.findByUserid(userid);

        if (findUser.isPresent()) {
            sentrySupport.logSimpleMessage((ExceptionEnum.DUPLICATED_USER_ID).getMessage());
            throw new ApiException(ExceptionEnum.DUPLICATED_USER_ID);
        }

        LocalDateTime now = LocalDateTime.now();
        int hour = now.getHour();
        int minute = now.getMinute();
        int second = now.getSecond();
        int millis = now.get(ChronoField.MILLI_OF_SECOND);

        String image_url = "https://chattingroom.s3.ap-northeast-2.amazonaws.com/S3image171054617.jpg";

        if (image != null) {
            String newFileName = "image" + hour + minute + second + millis;
            String fileExtension = '.' + image.getOriginalFilename().replaceAll("^.*\\.(.*)$", "$1");
            String imageName = S3_BUCKET_PREFIX + newFileName + fileExtension;

            String[] extensionArray = {".png", ".jpeg", ".jpg", ".webp", ".gif"};

            List<String> extensionList = new ArrayList<>(Arrays.asList(extensionArray));

            if (!extensionList.contains(fileExtension)) {
                throw new ApiException(ExceptionEnum.UNAUTHORIZED_FILE);
            }

            if (image.getSize() > 20971520) {
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

        UserRoleEnum role = UserRoleEnum.USER;

        userRepository.save(new User(userid, password, username,
                role, image_url, birthday, requestDto.getComment()));
        return new StatusResponseDto("회원가입 성공");
    }

    public StatusResponseDto login(UserRequestDto requestDto, HttpServletResponse response) {
        String userid = requestDto.getUserid();
        String password = requestDto.getPassword();

        Optional<User> user = userRepository.findByUserid(userid);

        if (user.isEmpty()) {
            sentrySupport.logSimpleMessage((ExceptionEnum.NOT_FOUND_USER).getMessage());
            throw new ApiException(ExceptionEnum.NOT_FOUND_USER);
        }

        if (!passwordEncoder.matches(password, user.get().getPassword())) {
            sentrySupport.logSimpleMessage((ExceptionEnum.BAD_REQUEST).getMessage());
            throw new ApiException(ExceptionEnum.BAD_REQUEST);
        }

        TokenDto tokenDto = jwtUtil.creatAllToken(userid, user.get().getRole());

        Optional<RefreshToken> refreshToken = refreshTokenRepository.findByUserid(userid);

        if (refreshToken.isPresent()) {
            refreshTokenRepository.save(refreshToken.get().updateToken(tokenDto.getRefreshToken()));
        } else {
            RefreshToken newToken = new RefreshToken(tokenDto.getRefreshToken(), userid);
            refreshTokenRepository.save(newToken);
        }

        response.addHeader(JwtUtil.ACCESS_KEY, tokenDto.getAccessToken());
        response.addHeader(JwtUtil.REFRESH_KEY, tokenDto.getRefreshToken());

        return new StatusResponseDto("로그인 성공");
    }

    public StatusResponseDto logout(User user) {
        RefreshToken refreshToken = refreshTokenRepository.findByUserid(user.getUserid()).orElseThrow(
                () -> new IllegalArgumentException("리프레시 토큰 없습니다.")
        );
        refreshTokenRepository.delete(refreshToken);
        return new StatusResponseDto("로그아웃 성공");
    }

    @Transactional(readOnly = true)
    public List<UserResponseDto> getUsers(String userid) {
        Optional<User> findUser = userRepository.findByUserid(userid);
        if (findUser.isPresent()) {
            List<User> userList = userRepository.findAllByOrderByUsernameDesc();
            return userList.stream().map(UserResponseDto::new).collect(Collectors.toList());
        }
        sentrySupport.logSimpleMessage((ExceptionEnum.UNAUTHORIZED).getMessage());
        throw new ApiException(ExceptionEnum.UNAUTHORIZED);
    }

    @Transactional(readOnly = true)
    public UserResponseDto findUserInfo(String userid) {
        Optional<User> findUser = userRepository.findByUserid(userid);

        if (findUser.isPresent()) {
            return new UserResponseDto(findUser.get());
        }
        sentrySupport.logSimpleMessage((ExceptionEnum.NOT_FOUND_USER).getMessage());
        throw new ApiException(ExceptionEnum.NOT_FOUND_USER);
    }

    @Transactional(readOnly = true)
    public ResponseEntity<UserResponseDto> myPage(User user) {
        return ResponseEntity.status(HttpStatus.OK).body(new UserResponseDto(user));
    }

    // 마이페이지 수정
    @Transactional
    public ResponseEntity<UserResponseDto> updateMypage(UserRequestDto userRequestDto, User user) throws IOException {

        User findUser = userRepository.findById(user.getId()).orElseThrow(
                () -> new ApiException(ExceptionEnum.NOT_FOUND_USER)
        );


        if (!userRequestDto.getPassword().matches("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{6,}$")) { // 비밀번호 정규식 체크
            sentrySupport.logSimpleMessage((ExceptionEnum.PASSWAORD_REGEX).getMessage());
            throw new ApiException(ExceptionEnum.PASSWAORD_REGEX);
        }
        String password = passwordEncoder.encode(userRequestDto.getPassword());
        userRequestDto.setPassword(password);
        findUser.update(userRequestDto);
        return ResponseEntity.status(HttpStatus.OK).body(new UserResponseDto(user));

    }

//    @Transactional(readOnly = true)
//    public List<UserResponseDto> checkUserByBirthday() {
//        List<User> userList = userRepository.findByUserAndBirthday();
//        return userList.stream().map(UserResponseDto::new).collect(Collectors.toList());
//    }

    @Transactional(readOnly = true)
    public ResponseDto userCheck(String userId) {
        Optional<User> found = userRepository.findByUserid(userId);
        String namePattern = "[a-zA-Z0-9]{4,12}$";
        int chk = userId.length();
        if (found.isPresent()) {
            return ResponseDto.set(BAD_REQUEST, "아이디 중복", null);
        } else {
            if (!userId.matches(namePattern)) {
                return ResponseDto.set(BAD_REQUEST, "소문자와 숫자만 입력 가능합니다.", null);
            } else if (chk < 4) {
                return ResponseDto.set(BAD_REQUEST, "id 크기는 4 이상, 12 이하만 가능합니다.", null);
            } else if (chk > 10) {
                return ResponseDto.set(BAD_REQUEST, "id 크기는 4 이상, 12 이하만 가능합니다.", null);
            } else {
                return ResponseDto.set(OK, "사용가능한 아이디 입니다.", userId);
            }
        }
    }
}
