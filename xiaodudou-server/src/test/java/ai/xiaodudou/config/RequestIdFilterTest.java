package ai.xiaodudou.config;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class RequestIdFilterTest {

    @Test
    void acceptsSafeRequestIdAndAlwaysClearsMdc() throws Exception {
        RequestIdFilter filter = new RequestIdFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(RequestIdFilter.HEADER, "client-request-123");
        AtomicReference<String> duringChain = new AtomicReference<>();

        filter.doFilter(request, response, (req, res) -> duringChain.set(MDC.get(RequestIdFilter.MDC_KEY)));

        assertThat(duringChain.get()).isEqualTo("client-request-123");
        assertThat(response.getHeader(RequestIdFilter.HEADER)).isEqualTo("client-request-123");
        assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void replacesUnsafeRequestIdWithGeneratedValue() throws Exception {
        RequestIdFilter filter = new RequestIdFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(RequestIdFilter.HEADER, "bad value\nforged");

        filter.doFilter(request, response, (req, res) -> { });

        assertThat(response.getHeader(RequestIdFilter.HEADER)).matches("[a-f0-9]{32}");
        assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void clearsMdcEvenWhenDownstreamThrows() {
        RequestIdFilter filter = new RequestIdFilter();

        try {
            filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(),
                    (req, res) -> { throw new IllegalStateException("boom"); });
        } catch (Exception ignored) {
            // 断言 finally 行为。
        }
        assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull();
    }
}
