package ai.xiaodudou.module.order.controller;

import ai.xiaodudou.common.exception.BusinessException;
import ai.xiaodudou.module.order.mapper.OrderMapper;
import ai.xiaodudou.module.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class OrderControllerSafetyTest {

    private OrderMapper orderMapper;
    private UserMapper userMapper;
    private StringRedisTemplate redis;
    private OrderController controller;

    @BeforeEach
    void setUp() {
        orderMapper = mock(OrderMapper.class);
        userMapper = mock(UserMapper.class);
        redis = mock(StringRedisTemplate.class);
        controller = new OrderController(orderMapper, userMapper, redis);
    }

    @Test
    void commercialFeatureShouldBeClosedByDefault() {
        ReflectionTestUtils.setField(controller, "commercialEnabled", false);

        assertThatThrownBy(controller::packages)
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("暂未开放");
        verifyNoInteractions(orderMapper, userMapper, redis);
    }

    @Test
    void enablingFlagsMustNotCreateOrderBeforeRealPaymentExists() {
        ReflectionTestUtils.setField(controller, "commercialEnabled", true);
        ReflectionTestUtils.setField(controller, "wechatPayEnabled", true);
        OrderController.CreateOrderReq request = new OrderController.CreateOrderReq();
        request.setPackageCode("POSTPARTUM_CARD");

        assertThatThrownBy(() -> controller.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("真实支付能力尚未完成");
        verifyNoInteractions(orderMapper, userMapper, redis);
    }

    @Test
    void refundMustRemainBlockedEvenWhenCommercialFlagIsEnabled() {
        ReflectionTestUtils.setField(controller, "commercialEnabled", true);
        OrderController.RefundReq request = new OrderController.RefundReq();
        request.setReason("测试");

        assertThatThrownBy(() -> controller.refund(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("真实退款能力尚未完成");
        verifyNoInteractions(orderMapper, userMapper, redis);
    }
}
