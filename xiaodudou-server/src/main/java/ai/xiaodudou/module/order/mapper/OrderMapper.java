package ai.xiaodudou.module.order.mapper;

import ai.xiaodudou.module.order.entity.Order;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

public interface OrderMapper extends BaseMapper<Order> {
    /** 仅删除非财务订单；PAID/REFUNDED 必须保留。 */
    @Delete("""
            DELETE FROM t_order
            WHERE user_id = #{userId}
              AND status IN ('PENDING', 'CANCELLED', 'EXPIRED')
            """)
    int deleteNonFinancialByUserId(@Param("userId") Long userId);
}
