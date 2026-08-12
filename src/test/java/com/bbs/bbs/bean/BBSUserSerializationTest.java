package com.bbs.bbs.bean;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class BBSUserSerializationTest {

    @Test
    void passwordHashIsExcludedFromRedisJson() {
        BBSUser user = new BBSUser();
        user.setUserId(1L);
        user.setLoginName("user@example.com");
        user.setPasswordHash("secret-bcrypt-hash");

        GenericJacksonJsonRedisSerializer serializer =
                GenericJacksonJsonRedisSerializer.builder()
                        .enableUnsafeDefaultTyping()
                        .build();
        String json = new String(serializer.serialize(user), StandardCharsets.UTF_8);

        assertThat(json).doesNotContain("passwordHash", "secret-bcrypt-hash");
    }
}
