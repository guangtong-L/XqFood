package ai.xiaodudou.module.order.controller;

import ai.xiaodudou.common.exception.BusinessException;
import ai.xiaodudou.common.result.Result;
import ai.xiaodudou.common.result.ResultCode;
import ai.xiaodudou.module.order.VipPackage;
import ai.xiaodudou.module.order.entity.Order;
import ai.xiaodudou.module.order.mapper.OrderMapper;
import ai.xiaodudou.module.user.entity.User;
import ai.xiaodudou.module.user.mapper.UserMapper;
import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 订单 + 会员
 *
 * 当前商业化、支付与退款均默认关闭。历史订单只读逻辑保留；在真实支付、验签回调与
 * 退款能力完成前，所有资金相关写接口均由服务端闸门拒绝。
 *
 * 历史实现仍保留订单防重、CAS 状态更新及 VIP 重算逻辑，便于未来完成真实支付后复核复用。
 */
@Slf4j
@Tag(name = "08 - 订单与会员")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class OrderController {

    private final OrderMapper orderMapper;
    private final UserMapper userMapper;
    private final StringRedisTemplate redis;

    @Value("${xiaodudou.commercial.enabled:false}")
    private boolean commercialEnabled;

    @Value("${xiaodudou.wechat.pay.enabled:false}")
    private boolean wechatPayEnabled;

    @Value("${xiaodudou.order.expire-minutes:30}")
    private int orderExpireMinutes;

    private static final Duration CREATE_LOCK_TTL = Duration.ofSeconds(5);

    // ============ 套餐列表 ============
    @SaIgnore
    @GetMapping("/packages")
    @Operation(summary = "套餐列表（公开）")
    public Result<List<Map<String, Object>>> packages() {
        assertCommercialEnabled();
        List<Map<String, Object>> list = new ArrayList<>();
        for (VipPackage p : VipPackage.all()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("code", p.getCode());
            m.put("name", p.getName());
            m.put("amountFen", p.getAmountFen());
            m.put("amountYuan", p.getAmountFen() / 100.0);
            m.put("vipLevel", p.getVipLevel());
            m.put("validDays", p.getValidDays());
            m.put("benefits", p.getBenefits());
            list.add(m);
        }
        return Result.ok(list);
    }

    // ============ 创建订单 ============
    @Data
    public static class CreateOrderReq {
        private String packageCode;
    }

    @PostMapping("/orders")
    @Operation(summary = "创建订单（当前由商业化闸门关闭）")
    @Transactional(rollbackFor = Exception.class)
    public Result<Map<String, Object>> create(@RequestBody CreateOrderReq req) {
        assertPaymentWriteAvailable();
        Long userId = StpUtil.getLoginIdAsLong();
        VipPackage pkg = VipPackage.of(req.getPackageCode());
        if (pkg == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "无效套餐 " + req.getPackageCode());
        }

        // A4: Redis 锁 —— 同用户同套餐 5s 冷却窗口（堵双击 + 并发 + 短时重复）
        // 注意：故意不在 finally 释放锁，让 TTL 自然过期，形成 5s 真正冷却
        // 副作用：合法用户最快 5s 后才能再次发起同套餐订单（可接受）
        String lockKey = "lock:order:create:" + userId + ":" + pkg.getCode();
        Boolean locked = redis.opsForValue().setIfAbsent(lockKey, "1", CREATE_LOCK_TTL);
        if (!Boolean.TRUE.equals(locked)) {
            log.info("[Order] 5s 冷却拦截 userId={} pkg={}", userId, pkg.getCode());
            throw new BusinessException(ResultCode.BAD_REQUEST, "请勿重复提交，请稍后再试");
        }

        // 1. 防重：同用户 PENDING 订单（同套餐）
        Order pending = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .eq(Order::getPackageCode, pkg.getCode())
                .eq(Order::getStatus, Order.STATUS_PENDING)
                .gt(Order::getExpireAt, LocalDateTime.now())
                .last("limit 1"));
        if (pending != null) {
            log.info("[Order] 复用待支付订单 outTradeNo={}", pending.getOutTradeNo());
            return Result.ok(buildPayResponse(pending));
        }

        // 2. 创建订单
        Order order = new Order();
        order.setOutTradeNo("XDD" + IdUtil.fastSimpleUUID().substring(0, 16).toUpperCase());
        order.setUserId(userId);
        order.setPackageCode(pkg.getCode());
        order.setPackageName(pkg.getName());
        order.setAmountFen(pkg.getAmountFen());
        order.setVipLevel(pkg.getVipLevel());
        order.setValidDays(pkg.getValidDays());
        order.setStatus(Order.STATUS_PENDING);
        order.setExpireAt(LocalDateTime.now().plusMinutes(orderExpireMinutes));
        orderMapper.insert(order);
        log.info("[Order] 创建订单 outTradeNo={} userId={} pkg={} amount={}",
                order.getOutTradeNo(), userId, pkg.getCode(), pkg.getAmountFen());

        // 3. 支付模式分支
        // 防御性兜底：当前版本没有真实支付实现，事务会回滚，绝不生成 PAID 订单。
        throw new BusinessException(ResultCode.FEATURE_NOT_AVAILABLE, "支付功能暂未开放");
    }

    /**
     * 历史支付成功状态处理逻辑（当前没有任何公开入口调用）
     * A5: 用 CAS 写订单（PENDING → PAID），并发/重复调用时幂等返回
     * A3: 通过 recalcUserVip 重算 VIP（不再就地累加）
     */
    private void paySuccess(Order order, String channel, String txId) {
        LocalDateTime now = LocalDateTime.now();
        int affected = orderMapper.update(null, Wrappers.<Order>lambdaUpdate()
                .eq(Order::getId, order.getId())
                .eq(Order::getStatus, Order.STATUS_PENDING)
                .set(Order::getStatus, Order.STATUS_PAID)
                .set(Order::getPayChannel, channel)
                .set(Order::getPayTransactionId, txId)
                .set(Order::getPaidAt, now));
        if (affected == 0) {
            log.warn("[Order] paySuccess CAS 失败，订单已被处理 outTradeNo={}", order.getOutTradeNo());
            return;
        }
        // 同步内存对象，让 buildPayResponse 拿到最新状态
        order.setStatus(Order.STATUS_PAID);
        order.setPayChannel(channel);
        order.setPayTransactionId(txId);
        order.setPaidAt(now);

        recalcUserVip(order.getUserId());
        log.info("[Order] 支付成功 outTradeNo={} userId={}", order.getOutTradeNo(), order.getUserId());
    }

    /**
     * A3: 按所有 STATUS_PAID 订单重算用户 VIP
     *   - vipLevel = max(订单.vipLevel)
     *   - vipExpireAt = 最早 paid_at + sum(订单.validDays)
     * 退款 / 支付成功后都走这里，保证 t_user 与 t_order 一致
     */
    private void recalcUserVip(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) return;

        List<Order> paidOrders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .eq(Order::getStatus, Order.STATUS_PAID)
                .orderByAsc(Order::getPaidAt));

        if (paidOrders.isEmpty()) {
            user.setVipLevel(0);
            user.setVipExpireAt(null);
        } else {
            int maxLevel = paidOrders.stream().mapToInt(Order::getVipLevel).max().orElse(0);
            long totalDays = paidOrders.stream().mapToLong(Order::getValidDays).sum();
            LocalDateTime base = paidOrders.get(0).getPaidAt();
            user.setVipLevel(maxLevel);
            user.setVipExpireAt(base.plusDays(totalDays));
        }
        userMapper.updateById(user);
        log.info("[Order] 重算 VIP userId={} level={} expireAt={}",
                userId, user.getVipLevel(), user.getVipExpireAt());
    }

    private Map<String, Object> buildPayResponse(Order order) {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("orderId", order.getId());
        resp.put("outTradeNo", order.getOutTradeNo());
        resp.put("amountFen", order.getAmountFen());
        resp.put("amountYuan", order.getAmountFen() / 100.0);
        resp.put("status", order.getStatus());
        resp.put("payChannel", order.getPayChannel());
        resp.put("expireAt", order.getExpireAt());
        return resp;
    }

    // ============ 我的订单 ============
    @GetMapping("/orders")
    @Operation(summary = "我的订单列表")
    public Result<Map<String, Object>> myOrders(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        Long userId = StpUtil.getLoginIdAsLong();
        Page<Order> p = orderMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getUserId, userId)
                        .orderByDesc(Order::getCreatedAt));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("records", p.getRecords());
        data.put("total", p.getTotal());
        return Result.ok(data);
    }

    @GetMapping("/orders/{id}")
    @Operation(summary = "订单详情")
    public Result<Order> detail(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        Order order = orderMapper.selectById(id);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        return Result.ok(order);
    }

    // ============ 历史退款逻辑（当前由服务端闸门关闭） ============
    @Data
    public static class RefundReq {
        private String reason;
    }

    @PostMapping("/orders/{id}/refund")
    @Operation(summary = "申请退款（当前未开放）")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> refund(@PathVariable Long id, @RequestBody RefundReq req) {
        assertRefundWriteAvailable();
        Long userId = StpUtil.getLoginIdAsLong();
        Order order = orderMapper.selectById(id);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        if (!Order.STATUS_PAID.equals(order.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "订单状态不可退款");
        }
        if (order.getPaidAt() != null && order.getPaidAt().plusDays(7).isBefore(LocalDateTime.now())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "已超过 7 天退款期");
        }

        // A5: CAS 防并发双退（status=PAID → REFUNDED 受影响行=0 则放弃）
        int affected = orderMapper.update(null, Wrappers.<Order>lambdaUpdate()
                .eq(Order::getId, id)
                .eq(Order::getStatus, Order.STATUS_PAID)
                .set(Order::getStatus, Order.STATUS_REFUNDED)
                .set(Order::getRefundedAt, LocalDateTime.now())
                .set(Order::getRefundReason, req.getReason()));
        if (affected == 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "订单状态已变更，请刷新后重试");
        }

        // A3: 按剩余 PAID 订单重算 VIP（不再粗暴清零）
        recalcUserVip(userId);
        log.info("[Order] 退款成功 outTradeNo={} userId={} reason={}",
                order.getOutTradeNo(), userId, req.getReason());
        return Result.ok();
    }

    private void assertCommercialEnabled() {
        if (!commercialEnabled) {
            throw new BusinessException(ResultCode.FEATURE_NOT_AVAILABLE, "会员与订单功能暂未开放");
        }
    }

    private void assertPaymentWriteAvailable() {
        assertCommercialEnabled();
        if (!wechatPayEnabled) {
            throw new BusinessException(ResultCode.FEATURE_NOT_AVAILABLE, "支付功能暂未开放");
        }
        // 真实微信支付接口、签名验签和回调处理尚未实现，禁止仅靠配置误开启。
        throw new BusinessException(ResultCode.FEATURE_NOT_AVAILABLE, "真实支付能力尚未完成，禁止创建订单");
    }

    private void assertRefundWriteAvailable() {
        assertCommercialEnabled();
        // 真实退款 API、异步通知及资金状态核对尚未实现，禁止把申请直接记为退款成功。
        throw new BusinessException(ResultCode.FEATURE_NOT_AVAILABLE, "真实退款能力尚未完成，请勿提交退款");
    }
}
