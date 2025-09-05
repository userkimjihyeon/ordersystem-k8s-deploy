package com.example.ordersystem.common.config;

import com.example.ordersystem.common.service.SseAlarmService;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {
//    db연결객체 : yml에서 받아온 host, post 값들을 조립해서 return
    @Value("${spring.redis.host}")
    private String host;
    @Value("${spring.redis.port}")
    private int port;
    @Bean
//    @Qualifier : 같은 Bean객체가 여러개 있을경우 Bean객체를 구분하기 위한 어노테이션
    @Qualifier("rtInventory")
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        configuration.setHostName(host);
        configuration.setPort(port);
        configuration.setDatabase(0);
        return new LettuceConnectionFactory(configuration);
    }
//    타입지정객체 : 키, 밸류(String, String)   => 얘를 jwtTokenProvider에서 사용
//    0번db(rtInventory)와 연결
    @Bean
    @Qualifier("rtInventory")
//    Bean들끼리 서로 의존성을 주입받을때, 메서드 파라미터로 주입가능
//    모든 template중에 무조건 redisTemplate 이라는 메서드명이 반드시 1개는 있어야 함.
    public RedisTemplate<String, String> redisTemplate(@Qualifier("rtInventory") RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        return redisTemplate;
    }
//    redis pub/sub을 위한 연결객체 생성
    @Bean
    @Qualifier("ssePubSub")
    public RedisConnectionFactory sseFactory() {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        configuration.setHostName(host);
        configuration.setPort(port);
//        redis pub/sub기능은 db에 값을 저장하는 기능이 아니므로, 특정 db에 의존적이지 않음.
        return new LettuceConnectionFactory(configuration);
    }
    @Bean
    @Qualifier("ssePubSub")
    public RedisTemplate<String, String> sseRedisTemplate(@Qualifier("ssePubSub") RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        return redisTemplate;
    }

//    redis 리스너 객체
    @Bean
    @Qualifier("ssePubSub")
    public RedisMessageListenerContainer redisMessageListenerContainer(
            @Qualifier("ssePubSub") RedisConnectionFactory redisConnectionFactory,
            MessageListenerAdapter messageListenerAdapter) {
       RedisMessageListenerContainer container = new RedisMessageListenerContainer();
       container.setConnectionFactory(redisConnectionFactory);
       container.addMessageListener(messageListenerAdapter, new PatternTopic("order-channel"));
//       만약에 여러 채널을 구독해야하는경우, 여러개의 patternTopic을 add하거나, 별도의 Bean객체 생성.
       return container;
    }

//    redis의 채널에서 수신된 메시지를 처리하는 빈객체
    @Bean
    public MessageListenerAdapter messageListenerAdapter(SseAlarmService sseAlarmService) {
//        채널로부터 수신되는 message처리를 SseAlarmService의 onMessage메서드로 설정
//        즉, 메시지가 수신되면 onMessage메서드가 호출
        return new MessageListenerAdapter(sseAlarmService, "onMessage");
    }
}
