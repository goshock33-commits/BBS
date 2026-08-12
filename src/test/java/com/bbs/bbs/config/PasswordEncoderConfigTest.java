package com.bbs.bbs.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordEncoderConfigTest {
    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withUserConfiguration(PasswordEncoderConfig.class);

    @Test
    void exposesBcryptPasswordEncoder() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(PasswordEncoder.class);
            assertThat(context.getBean(PasswordEncoder.class))
                    .isInstanceOf(BCryptPasswordEncoder.class);
        });
    }
}
