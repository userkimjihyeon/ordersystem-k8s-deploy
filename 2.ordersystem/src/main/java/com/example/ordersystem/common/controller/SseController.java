package com.example.ordersystem.common.controller;

import com.example.ordersystem.common.service.SseAlarmService;
import com.example.ordersystem.common.service.SseEmitterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/sse")
public class SseController {
    private final SseEmitterRegistry sseEmitterRegistry;

//    메시지 받을사람(admin)이 요청보냄
    @GetMapping("/connect")
    public SseEmitter subscribe() {
        SseEmitter sseEmitter = new SseEmitter(14400*60*1000L);   //10일정도 emitter유효기간 설정
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        sseEmitterRegistry.addSseEmitter(email, sseEmitter);       //sseEmitter를 add하는 메서드 호출

        try {
            sseEmitter.send(SseEmitter.event().name("connect").data("연결완료"));   //connect(라벨)
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return sseEmitter;  //메시지를 실시간으로 받을 수 있도록 리턴값설정
    }

    @GetMapping("/disconnect")
    public void unSubscribe() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        sseEmitterRegistry.removeEmitter(email);                  //sseEmitter를 romove하는 메서드 호출
    }
}
