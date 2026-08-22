package ai.xiaodudou.module.user.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClientSourceResolverTest {

    @Test
    void untrustedClientCannotSpoofForwardedAddress() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.10");
        request.addHeader("X-Forwarded-For", "198.51.100.9");

        assertThat(new ClientSourceResolver("").resolve(request)).isEqualTo("203.0.113.10");
    }

    @Test
    void trustedProxyUsesFirstValidForwardedAddress() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.2.3.4");
        request.addHeader("X-Forwarded-For", "198.51.100.9, 10.2.3.4");

        assertThat(new ClientSourceResolver("10.0.0.0/8").resolve(request)).isEqualTo("198.51.100.9");
    }

    @Test
    void invalidForwardedAddressFallsBackToTrustedProxyAddress() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.2.3.4");
        request.addHeader("X-Forwarded-For", "not-an-ip");

        assertTimeoutPreemptively(Duration.ofMillis(500), () ->
                assertThat(new ClientSourceResolver("10.0.0.0/8").resolve(request))
                        .isEqualTo("10.2.3.4"));
    }

    @Test
    void trustedProxyConfigurationRejectsHostnamesInsteadOfResolvingDns() {
        assertThatThrownBy(() -> new ClientSourceResolver("proxy.example.test/32"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("可信代理地址配置无效");
    }
}
