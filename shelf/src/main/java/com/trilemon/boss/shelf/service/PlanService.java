package com.trilemon.boss.shelf.service;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.*;
import com.taobao.api.domain.Item;
import com.taobao.api.request.ItemUpdateDelistingRequest;
import com.taobao.api.request.ItemUpdateListingRequest;
import com.taobao.api.request.ItemsOnsaleGetRequest;
import com.taobao.api.response.ItemUpdateDelistingResponse;
import com.taobao.api.response.ItemUpdateListingResponse;
import com.taobao.api.response.ItemsOnsaleGetResponse;
import com.trilemon.boss.infra.base.client.BaseClient;
import com.trilemon.boss.infra.base.model.TaobaoSession;
import com.trilemon.boss.infra.base.service.AppService;
import com.trilemon.boss.infra.base.service.TaobaoApiService;
import com.trilemon.boss.infra.base.service.api.TaobaoApiShopService;
import com.trilemon.boss.infra.base.service.api.exception.BaseTaobaoApiException;
import com.trilemon.boss.infra.base.service.api.exception.TaobaoAccessControlException;
import com.trilemon.boss.infra.base.service.api.exception.TaobaoEnhancedApiException;
import com.trilemon.boss.infra.base.service.api.exception.TaobaoSessionExpiredException;
import com.trilemon.boss.infra.base.util.TopApiUtils;
import com.trilemon.boss.shelf.ShelfConstants;
import com.trilemon.boss.shelf.ShelfException;
import com.trilemon.boss.shelf.ShelfUtils;
import com.trilemon.boss.shelf.dao.PlanMapper;
import com.trilemon.boss.shelf.dao.PlanSettingMapper;
import com.trilemon.boss.shelf.model.Plan;
import com.trilemon.boss.shelf.model.PlanSetting;
import com.trilemon.commons.DateUtils;
import com.trilemon.commons.LocalTimeInterval;
import com.trilemon.commons.mybatis.MyBatisBatchWriter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTime;
import org.joda.time.Minutes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.trilemon.boss.shelf.ShelfUtils.buildPlan;

/**
 * @author kevin
 */
@Service
public class PlanService {
    private final static Logger logger = LoggerFactory.getLogger(PlanService.class);
    @Autowired
    private PlanMapper planMapper;
    @Autowired
    private PlanSettingMapper planSettingMapper;
    @Autowired
    private AppService appService;
    @Autowired
    private TaobaoApiShopService taobaoApiShopService;
    @Autowired
    private BaseClient baseClient;
    @Autowired
    private TaobaoApiService taobaoApiService;
    @Autowired
    private MyBatisBatchWriter myBatisBatchWriter;

    /**
     * 更新一个计划。
     *
     * @throws ShelfException
     */
    public void updatePlan(Long planSettingId) throws TaobaoSessionExpiredException, TaobaoEnhancedApiException, TaobaoAccessControlException, ShelfException {
        PlanSetting planSetting = planSettingMapper.selectByPrimaryKey(planSettingId);
        checkNotNull(planSetting, "planSetting[%s] is null.", planSettingId);
        //所有在售宝贝

        ItemsOnsaleGetRequest request = new ItemsOnsaleGetRequest();
        request.setFields(Joiner.on(",").join(ShelfConstants.ITEM_FIELDS));
        request.setSellerCids(planSetting.getIncludeSellerCids());
        ItemsOnsaleGetResponse result = taobaoApiShopService.getOnSaleItems(planSetting.getUserId(), request);

        List<Item> onSaleItems = result.getItems();

        if (null == onSaleItems) {
            logger.info("onsale item is empty, userId[{}], PlanSettingId[{}]", planSetting.getUserId(),
                    planSetting.getId());
            return;
        }

        Set<Long> onSaleNumIids = Sets.newHashSet(TopApiUtils.getItemNumIids(onSaleItems));

        //所有需要排除宝贝
        Iterable<Long> excludeItemNumIids = null;

        if (StringUtils.isNotBlank(planSetting.getExcludeItemNumIids())) {
            excludeItemNumIids = Iterables.transform(Splitter.on(",").split(planSetting.getExcludeItemNumIids
                    ()),
                    new Function<String, Long>() {
                        @Nullable
                        @Override
                        public Long apply(@Nullable String input) {
                            return Long.valueOf(input);
                        }
                    });
        }

        //所有计划中宝贝
        List<Plan> runningPlans = planMapper.selectByPlanSettingId(planSetting.getId());
        final List<Long> runningPlanNumIids = ShelfUtils.getPlanNumIids(runningPlans);

        //正在计划中的并且在售的宝贝NumIid
        Set<Long> existAndOnSaleItemNumIids = Sets.intersection(Sets.newHashSet(onSaleNumIids),
                Sets.newHashSet(runningPlanNumIids));

        //计划中失效宝贝NumIid
        List<Long> invalidPlanItemNumIids = ListUtils.removeAll(runningPlanNumIids,
                existAndOnSaleItemNumIids);

        //需要加入计划的新加入宝贝NumIid
        List<Long> newItemNumIids = ListUtils.removeAll(onSaleNumIids, existAndOnSaleItemNumIids);

        //需要加入计划的新加入宝贝
        List<Item> newItems = ShelfUtils.getItems(onSaleItems, newItemNumIids);

        //1. 删除计划中失效宝贝
        if (CollectionUtils.isNotEmpty(invalidPlanItemNumIids)) {
            planMapper.deleteByUserIdAndNumIids(planSetting.getUserId(), invalidPlanItemNumIids);
        }

        //2. 加入新宝贝
        if (CollectionUtils.isEmpty(newItems)) {
            return;
        }
        List<Plan> validPlans = ShelfUtils.getPlans(runningPlans, existAndOnSaleItemNumIids);
        Table<Integer, LocalTimeInterval, Integer> currDistribution = ShelfUtils.getDistribution(validPlans);
        //为新添宝贝安排具体的调整时间
        Table<Integer, LocalTimeInterval, List<Item>> assignTable = avgAssignNewItems(newItems, planSetting,
                currDistribution);
        //安排计划
        List<Plan> plans = plan(planSetting, assignTable);

        //为排除宝贝计划生成标志位
        if (null != excludeItemNumIids) {
            for (Plan plan : plans) {
                if (Iterables.contains(excludeItemNumIids, plan.getItemNumIid())) {
                    plan.setStatus(ShelfConstants.PLAN_STATUS_EXCLUDED);
                }
            }
        }
        savePlan(planSetting.getId(), plans);
    }

    /**
     * 安排计划并保存
     *
     * @param planSetting
     * @throws ShelfException
     */
    public void createPlan(Long userId, PlanSetting planSetting) throws ShelfException, TaobaoSessionExpiredException, TaobaoAccessControlException, TaobaoEnhancedApiException {
        checkArgument(userId == planSetting.getUserId(), "userId[%s] is not equal with userId of planSetting[%s]",
                userId, planSetting.getUserId());
        ItemsOnsaleGetRequest request = new ItemsOnsaleGetRequest();
        request.setFields(Joiner.on(",").join(ShelfConstants.ITEM_FIELDS));
        request.setSellerCids(planSetting.getIncludeSellerCids());
        ItemsOnsaleGetResponse result = taobaoApiShopService.getOnSaleItems(userId, request);

        //获取已经计划的宝贝
        List<Long> usedItemNumIids = planMapper.selectNumIidsByUserId(userId);

        List<Plan> plans = plan(planSetting, TopApiUtils.excludeItems(result.getItems(), usedItemNumIids));
        logger.info("userId[{}] generate {} plans for planSettingId[{}].", userId, plans.size(), planSetting.getId());
        savePlan(planSetting.getId(), plans);
    }

    @Transactional
    private void savePlan(Long planSettingId, List<Plan> plans) {
        if (CollectionUtils.isNotEmpty(plans)) {
            for (Plan plan : plans) {
                plan.setAddTime(appService.getLocalSystemTime().toDate());
            }
            myBatisBatchWriter.write("com.trilemon.boss.shelf.dao.PlanMapper.insertSelective", plans);
        }
        PlanSetting planSetting = new PlanSetting();
        planSetting.setId(planSettingId);
        planSetting.setStatus(ShelfConstants.PLAN_SETTING_STATUS_RUNNING);
        planSetting.setLastPlanTime(appService.getLocalSystemTime().toDate());
        planSettingMapper.updateByPrimaryKeySelective(planSetting);
    }

    /**
     * 依据计划设置安排计划，并且返回计划列表。
     *
     * @param planSetting {@link PlanSetting}
     * @param items       需要纳入计划的宝贝
     * @return 不会返回 null，如果没有计划，返回一个空的集合
     */
    @NotNull
    private List<Plan> plan(PlanSetting planSetting, List<Item> items) throws ShelfException {
        Table<Integer, LocalTimeInterval, List<Item>> assignTable = null;
        switch (planSetting.getDistributionType()) {
            case ShelfConstants.PLAN_SETTING_DISTRIBUTE_TYPE_AUTO:
                assignTable = autoAssignItems(items);
                break;
            case ShelfConstants.PLAN_SETTING_DISTRIBUTE_TYPE_MANUAL:
                assignTable = manualAssignItems(planSetting, items);
                break;
        }
        if (null == assignTable) {
            throw new ShelfException("plan for userId[" + planSetting.getUserId() + "], planSettingId[" + planSetting
                    .getId() + "] error, assign table is null.");
        }
        return plan(planSetting, assignTable);
    }

    /**
     * 将新加入宝贝纳入计划
     *
     * @param planSetting 计划设置
     * @param assignTable 需要加入计划的宝贝的<周几，时段，宝贝>
     * @return
     */
    public List<Plan> plan(PlanSetting planSetting, Table<Integer, LocalTimeInterval, List<Item>> assignTable) {
        List<Plan> plans = Lists.newArrayList();

        DateTime now = appService.getLocalSystemTime();
        DateTime tomorrow = now.plusDays(1).withTimeAtStartOfDay();
        //第一次调整是周几
        DateTime firstAdjustDay = now;
        //如果离第二天小于10分钟，就把首次调整计划安排到第二天
        if (Minutes.minutesBetween(now, tomorrow).getMinutes() < 10) {
            firstAdjustDay = now.plusDays(1);
        }
        int firstAdjustDayOfWeek = firstAdjustDay.getDayOfWeek();
        for (Map.Entry<Integer, Map<LocalTimeInterval, List<Item>>> assignDay : assignTable.rowMap().entrySet()) {
            for (Map.Entry<LocalTimeInterval, List<Item>> assignHour : assignDay.getValue().entrySet()) {
                LocalTimeInterval assignHourTimeInterval = assignHour.getKey();
                List<Item> items = assignHour.getValue();
                for (Item item : items) {
                    int planListingDayOffset = assignDay.getKey() - firstAdjustDayOfWeek;
                    if (planListingDayOffset < 0) {
                        planListingDayOffset += 7;
                    }
                    DateTime planListingDateTime = firstAdjustDay.plusDays(planListingDayOffset);
                    try {
                        Plan plan = buildPlan(planSetting, item, planListingDateTime.withTimeAtStartOfDay(),
                                assignHourTimeInterval);
                        plans.add(plan);
                    } catch (ShelfException e) {
                        logger.error("createPlan error, planSettingID[" + planSetting.getId() + "]", e);
                    }
                }
            }
        }
        return plans;
    }

    private Table<Integer, LocalTimeInterval, List<Item>> avgAssignNewItems(List<Item> items,
                                                                            PlanSetting planSetting,
                                                                            Table<Integer, LocalTimeInterval, Integer> currDistribution)
            throws ShelfException {
        Table<Integer, LocalTimeInterval, Integer> planDistribution = null;
        switch (planSetting.getDistributionType()) {
            case ShelfConstants.PLAN_SETTING_DISTRIBUTE_TYPE_AUTO:
                planDistribution = ShelfUtils.getDefaultZeroFilledDistribution();
                break;
            case ShelfConstants.PLAN_SETTING_DISTRIBUTE_TYPE_MANUAL:
                try {
                    planDistribution = ShelfUtils.parseAndFillZeroDistribution(planSetting.getDistribution());
                } catch (Exception e) {
                    throw new ShelfException("parse distribution error, planSettingId[" + planSetting.getId() + "]", e);
                }
                break;
        }
        if (null == planDistribution) {
            throw new ShelfException("planDistribution is null, itemSize[" + items.size() + "], " +
                    "planSettingId[" + planSetting.getId() + "].");
        }
        Table<Integer, LocalTimeInterval, Integer> newItemDistribution = ShelfUtils.getNewItemDistribution(items.size(),
                planDistribution, currDistribution);
        return ShelfUtils.assignItems(items, newItemDistribution);
    }

    /**
     * 平均分布商品
     *
     * @param items
     * @return
     */
    private Table<Integer, LocalTimeInterval, List<Item>> autoAssignItems(List<Item> items) {
        Table<Integer, LocalTimeInterval, Integer> distribution = ShelfUtils.getDefaultDistribution(items.size());
        return ShelfUtils.assignItems(items, distribution);
    }

    /**
     * 平均分布商品到用户指定的时段
     *
     * @param planSetting
     * @param items
     * @return
     */
    public Table<Integer, LocalTimeInterval, List<Item>> manualAssignItems(PlanSetting planSetting, List<Item> items) throws ShelfException {
        Table<Integer, LocalTimeInterval, Integer> distribution;
        try {
            distribution = ShelfUtils.parseAndFillZeroDistribution(planSetting
                    .getDistribution());
        } catch (Exception e) {
            throw new ShelfException("parse distribution error, planSettingId[" + planSetting.getId() + "]", e);
        }
        return ShelfUtils.assignItems(items, distribution);
    }

    public void execPlan(Long planSettingId) throws TaobaoSessionExpiredException, TaobaoAccessControlException, TaobaoEnhancedApiException {
        DateTime now = appService.getLocalSystemTime();
        List<Plan> plans = planMapper.selectByPlanSettingIdAndStatusAndPlanTime(planSettingId,
                ImmutableList.of(ShelfConstants.PLAN_STATUS_WAITING_ADJUST, ShelfConstants.PLAN_STATUS_FAILED),
                now.withTimeAtStartOfDay().toDate(),
                now.toLocalTime().plusHours(1).toDateTimeToday().toDate());
        logger.info("planSetting[{}] get [{}] plan to adjust", planSettingId, plans.size());
        for (Plan plan : plans) {
            execPlan(plan);
        }
    }

    private void execPlan(Plan plan) throws TaobaoAccessControlException, TaobaoSessionExpiredException, TaobaoEnhancedApiException {
        DateTime now = appService.getLocalSystemTime();
        if (now.toLocalTime().getMillisOfDay() < plan.getPlanAdjustStartTime().getTime()) {
            logger.info("planId[{}] is delay, startTime[{}] but now[{}]", plan.getId(),
                    DateUtils.format(plan.getPlanAdjustEndTime(), DateUtils.yyyy_MM_dd_HH_mm_ss),
                    now.toString(DateUtils.yyyy_MM_dd_HH_mm_ss));
        }

        Plan planResult = new Plan();
        planResult.setId(plan.getId());
        ItemUpdateDelistingRequest request = new ItemUpdateDelistingRequest();
        request.setNumIid(plan.getItemNumIid());
        planResult.setAdjustTime(appService.getLocalSystemTime().toDate());
        TaobaoSession taobaoSession = baseClient.getTaobaoSession(plan.getUserId(), taobaoApiService.getAppKey());
        try {
            //下架
            ItemUpdateDelistingResponse delistingResponse = taobaoApiService.request(request,
                    taobaoSession.getAccessToken());
            if (delistingResponse.isSuccess() || delistingResponse.getSubCode().equals("isv.item-listing-service-error")) {
                //获取商品数量
                Item item = taobaoApiShopService.getItem(plan.getUserId(), plan.getItemNumIid(),
                        ImmutableList.of("num"));
                //上架
                ItemUpdateListingRequest listingRequest = new ItemUpdateListingRequest();
                listingRequest.setNumIid(plan.getItemNumIid());
                listingRequest.setNum(item.getNum());
                ItemUpdateListingResponse listingResponse = taobaoApiService.request(listingRequest,
                        taobaoSession.getAccessToken());
                if (listingResponse.isSuccess()) {
                    planResult.setFailedCause("");
                    planResult.setStatus(ShelfConstants.PLAN_STATUS_SUCCESSFUL);
                } else {
                    planResult.setStatus(ShelfConstants.PLAN_STATUS_FAILED);
                    planResult.setFailedCause(listingResponse.getSubCode() + listingResponse.getSubMsg());
                }
            } else {
                planResult.setStatus(ShelfConstants.PLAN_STATUS_FAILED);
                planResult.setFailedCause(delistingResponse.getSubCode() + delistingResponse.getSubMsg());
            }
        } catch (BaseTaobaoApiException e) {
            planResult.setStatus(ShelfConstants.PLAN_STATUS_FAILED);
            planResult.setFailedCause(ToStringBuilder.reflectionToString(e));
            planMapper.updateByPrimaryKeySelective(planResult);
            throw e;
        }
        planMapper.updateByPrimaryKeySelective(planResult);
    }

    public void deletePlan(Long userId, Long planSettingId) {
        planMapper.deleteByUserIdAndPlanSettingId(userId, planSettingId);
    }

    public void excludeItem(Long planSettingId, Long numIid) {
        Plan plan = new Plan();
        plan.setPlanSettingId(planSettingId);
        plan.setItemNumIid(numIid);
        plan.setStatus(ShelfConstants.PLAN_STATUS_EXCLUDED);
        planMapper.updateByPlanSettingIdAndNumIid(plan);
    }

    public void includeItem(Long planSettingId, Long numIid) {
        Plan plan = new Plan();
        plan.setPlanSettingId(planSettingId);
        plan.setItemNumIid(numIid);
        plan.setStatus(ShelfConstants.PLAN_STATUS_WAITING_ADJUST);
        planMapper.updateByPlanSettingIdAndNumIid(plan);
    }

    public void setAppService(AppService appService) {
        this.appService = appService;
    }
}