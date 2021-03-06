package com.trilemon.boss.infra.sync.rate.job;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.trilemon.boss.infra.base.service.AppService;
import com.trilemon.boss.infra.sync.rate.dao.SyncStatusDAO;
import com.trilemon.boss.infra.sync.rate.service.RateSyncService;
import com.trilemon.jobqueue.service.AbstractCronQueueService;
import com.trilemon.jobqueue.service.queue.JobQueue;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.trilemon.boss.infra.sync.rate.RateSyncConstants.*;

/**
 * 同步评论队列
 *
 * @author kevin
 */
//@Component
public class RateSyncJob extends AbstractCronQueueService<Long> {
    private final static Logger logger = LoggerFactory.getLogger(RateSyncJob.class);
    @Autowired
    private RateSyncService rateSyncService;
    @Autowired
    private SyncStatusDAO syncStatusDAO;
    @Autowired
    private AppService appService;
    @Autowired
    private JobQueue<Long> jobQueue;

    @PostConstruct
    public void init() {
        setJobQueue(jobQueue);
        setTag("job-queue[sync-rate]");
        setCron("0 0 1 * * ?");
        start();
        appService.addThreads(getThreadPoolExecutorMap());
        logger.info("add [{}] thread[{}] to monitor.", getThreadPoolExecutorMap().size(), getThreadPoolExecutorMap());
    }

    @Override
    public void clean() {
        Stopwatch stopwatch = Stopwatch.createStarted();
        logger.info("start clean...");
        rateSyncService.cleanExpiredSyncTasks();
        stopwatch.stop();
        logger.info("end clean, spend time [{}] sec", stopwatch.elapsed(TimeUnit.SECONDS));
    }

    @Override
    public void process(Long userId) {
        try {
            rateSyncService.sync(userId);
        } catch (Exception e) {
            logger.error("process rate sync job error, userId[" + userId + "]", e);
        }
    }

    @Override
    public void fillQueue() {
        logger.info("start to fill queue.");
        int elemCount = 0;
        long hitUserId = 0;
        while (true) {
            try {
                List<Long> userIds = syncStatusDAO.paginateUserIdByRateSyncStatus(hitUserId, 100,
                        ImmutableList.of(RATE_SYNC_STATUS_INIT,
                                RATE_SYNC_STATUS_FAILED,
                                RATE_SYNC_STATUS_SUCCESSFUL));
                if (CollectionUtils.isEmpty(userIds)) {
                    break;
                } else {
                    hitUserId = Iterables.getLast(userIds);
                    fillQueue(userIds);
                    elemCount += userIds.size();
                }
            } catch (Throwable e) {
                logger.error("poll update error", e);
            }
        }
        logger.info("end to fill queue[{}].", elemCount);
    }

    @Override
    public void pollNull() {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
