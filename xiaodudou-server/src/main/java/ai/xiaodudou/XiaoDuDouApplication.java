package ai.xiaodudou;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 小肚兜 AI 后端启动类
 *
 * @author xiaodudou
 * @since 2026-05-23
 */
@SpringBootApplication
@MapperScan("ai.xiaodudou.**.mapper")
public class XiaoDuDouApplication {

    public static void main(String[] args) {
        SpringApplication.run(XiaoDuDouApplication.class, args);
    }
}
