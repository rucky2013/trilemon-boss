package com.trilemon.boss.rate.dao.router;

import com.google.common.math.LongMath;
import com.trilemon.boss.rate.model.RateLog;
import com.trilemon.commons.db.ShardTableRouter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author kevin
 */
@Component
public class RateLogRouter extends ShardTableRouter<RateLog> {
    @Value("${rate_log_table_num}")
    private int tableNum;

    @Override
    public int route(RateLog rateLog) {
        checkNotNull(rateLog.getUserId(), "userId is null");
        return LongMath.mod(rateLog.getUserId(), tableNum) + 1;
    }
}
