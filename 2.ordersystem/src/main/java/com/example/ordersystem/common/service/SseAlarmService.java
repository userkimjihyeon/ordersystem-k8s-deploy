package com.example.ordersystem.common.service;

import com.example.ordersystem.common.dto.SseMessageDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class SseAlarmService implements MessageListener {
    private final SseEmitterRegistry sseEmitterRegistry;
    private final RedisTemplate<String, String> redisTemplate;

//    생성자
    public SseAlarmService(SseEmitterRegistry sseEmitterRegistry, @Qualifier("ssePubSub") RedisTemplate<String, String> redisTemplate) {
        this.sseEmitterRegistry = sseEmitterRegistry;
        this.redisTemplate = redisTemplate;
    }

    //    특정 사용자에게 message발송
    public void publishMessage(String receiver, String sender, Long orderingId) {    //추후 orderingId에는 알림메시지가 들어와야함
//        메세지 dto 조립
        SseMessageDto dto = SseMessageDto.builder()
                .sender(sender)
                .receiver(receiver)
                .orderingId(orderingId)
                .build();

        ObjectMapper objectMapper = new ObjectMapper();
        String data = null;
        try {
            data = objectMapper.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

//        emitter객체를 통해 메시지 전송
        SseEmitter sseEmitter = sseEmitterRegistry.getEmitter(receiver);   //receiver email로 emitter객체를 꺼내서
//        emitter객체가 현재 서버에 있으면, 직접 알림 발송. 그렇지 않으면, redis에 publish.
        if(sseEmitter != null) {
            try {
                sseEmitter.send(SseEmitter.event().name("ordered").data(data));  //ordered(라벨) 및 메시지를 보낸다
            } catch (IOException e) {
                e.printStackTrace();    //알림보내줄사람을 못찾았음 -> 에러터트리지않고 메시지만 출력
            }
//        사용자가 로그아웃(새로고침) 후에 다시 화면에 들어왔을때 알림메시지가 남아있으려면 DB에 추가적으로 저장 필요
        } else {
            redisTemplate.convertAndSend("order-channel", data);    //주문'채널'에 data메세지 전송
        }

    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
//        Message : 실질적인 메시지가 담겨있는 객체
//        Pattern : 채널명
        String channel_name = new String(pattern);
//        여러개의 채널을 구독하고 있을경우, 채널명으로 분기처리
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            SseMessageDto dto = objectMapper.readValue(message.getBody(), SseMessageDto.class);
            SseEmitter sseEmitter = sseEmitterRegistry.getEmitter(dto.getReceiver());
            if(sseEmitter != null) {
                try {
                    sseEmitter.send(SseEmitter.event().name("ordered").data(dto));  //ordered(라벨) 및 메시지를 보낸다
                } catch (IOException e) {
                    e.printStackTrace();    //알림보내줄사람을 못찾았음 -> 에러터트리지않고 메시지만 출력
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }



    }
}
