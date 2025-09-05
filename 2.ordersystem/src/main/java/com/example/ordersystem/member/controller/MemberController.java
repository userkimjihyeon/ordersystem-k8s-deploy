package com.example.ordersystem.member.controller;

import com.example.ordersystem.common.auth.JwtTokenProvider;
import com.example.ordersystem.common.dto.CommonDto;
import com.example.ordersystem.member.dto.*;
import com.example.ordersystem.member.domain.Member;
import com.example.ordersystem.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/member")
public class MemberController {
    private final MemberService memberService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/create")
    public ResponseEntity<?> save(@RequestBody @Valid MemberCreateDto memberCreateDto) {
        Long id = this.memberService.save(memberCreateDto);
        return new ResponseEntity<>(
                CommonDto.builder()
                        .result(id)
                        .status_code(HttpStatus.CREATED.value())
                        .status_message("회원가입 완료")
                        .build()
                , HttpStatus.CREATED);
    }
    @PostMapping("/doLogin")
    public ResponseEntity<?> doLogin(@RequestBody LoginReqDto loginReqDto) {
        Member member = this.memberService.login(loginReqDto);
//        at 토큰 생성
        String accessToken = jwtTokenProvider.createAtToken(member);
//        rt 토큰 생성
        String refreshToken = jwtTokenProvider.createRtToken(member);

        LoginResDto loginResDto = LoginResDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();

        return new ResponseEntity<>(
                CommonDto.builder()
                        .result(loginResDto)
                        .status_code(HttpStatus.OK.value())
                        .status_message("로그인 성공")
                        .build()
                , HttpStatus.OK);
    }
//    rt를 통한 at 갱신 요청
    @PostMapping("/refresh-at")
    public ResponseEntity<?> generateNewAt(@RequestBody RefreshTokenDto refreshTokenDto) {
//        rt 검증 로직
        Member member = jwtTokenProvider.validateRt(refreshTokenDto.getRefreshToken());
//        at 신규 생성
        String accessToken = jwtTokenProvider.createAtToken(member);
        LoginResDto loginResDto = LoginResDto.builder()
                .accessToken(accessToken)
                .build();

        return new ResponseEntity<>(
                CommonDto.builder()
                        .result(loginResDto)
                        .status_code(HttpStatus.OK.value())
                        .status_message("at 재발급 성공")
                        .build()
                , HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/list")
    public ResponseEntity<?> findAll(){
        List<MemberResDto> memberResDtoList = memberService.findAll();
        return new ResponseEntity<>(
                CommonDto.builder()
                        .result(memberResDtoList)
                        .status_code(HttpStatus.OK.value())
                        .status_message("회원목록조회완료")
                        .build(), HttpStatus.OK);
    }
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/detail/{id}")
    public ResponseEntity<?> findById(@PathVariable("id") Long id){
        return new ResponseEntity<>(
                CommonDto.builder()
                        .result(memberService.findById(id))
                        .status_code(HttpStatus.OK.value())
                        .status_message("회원상세조회완료")
                        .build(), HttpStatus.OK);
    }
    @GetMapping("/myinfo")
    public ResponseEntity<?> myInfo() {
        return new ResponseEntity<>(
                CommonDto.builder()
                        .result(memberService.myinfo())
                        .status_code(HttpStatus.OK.value())
                        .status_message("내정보조회완료")
                        .build(),
                HttpStatus.OK);
    }
    @DeleteMapping("/delete")
    public ResponseEntity<?> delete() {
        memberService.delete();
        return new ResponseEntity<>(
                CommonDto.builder()
                        .result("OK")
                        .status_code(HttpStatus.OK.value())
                        .status_message("회원탈퇴완료")
                        .build(),
                HttpStatus.OK);
    }
}
