package likelion13th.shop.global.config;

/* 서버가 뚫려도 비밀번호를 알 수 없도록 하는 PasswordEncoder!
   암호화 및 검증하는 역할이기 때문에 필수다 */

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordEncoderConfig {
    /* Bean 빼놓으면 클남 */
    @Bean
    // 비밀번호 암호화를 위한 Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
