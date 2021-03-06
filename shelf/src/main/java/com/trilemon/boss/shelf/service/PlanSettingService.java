package com.trilemon.boss.shelf.service;

import com.google.common.base.Predicate;
import com.google.common.collect.*;
import com.taobao.api.request.ItemsOnsaleGetRequest;
import com.taobao.api.response.ItemsOnsaleGetResponse;
import com.trilemon.boss.center.PlanDistributionUtils;
import com.trilemon.boss.infra.base.model.dto.SellerCatExtended;
import com.trilemon.boss.infra.base.service.AppService;
import com.trilemon.boss.infra.base.service.api.TaobaoApiShopService;
import com.trilemon.boss.infra.base.service.api.exception.TaobaoAccessControlException;
import com.trilemon.boss.infra.base.service.api.exception.TaobaoEnhancedApiException;
import com.trilemon.boss.infra.base.service.api.exception.TaobaoSessionExpiredException;
import com.trilemon.boss.shelf.ShelfConstants;
import com.trilemon.boss.shelf.ShelfException;
import com.trilemon.boss.shelf.ShelfUtils;
import com.trilemon.boss.shelf.dao.PlanMapper;
import com.trilemon.boss.shelf.dao.PlanSettingMapper;
import com.trilemon.boss.shelf.model.Plan;
import com.trilemon.boss.shelf.model.PlanSetting;
import com.trilemon.boss.shelf.model.dto.PlanStatus;
import com.trilemon.boss.shelf.model.dto.ShelfStatus;
import com.trilemon.commons.Collections3;
import com.trilemon.commons.JsonMapper;
import com.trilemon.commons.Languages;
import com.trilemon.commons.web.Page;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
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

import static com.trilemon.boss.shelf.ShelfConstants.*;

/**
 * @author kevin
 */
@Service
public class PlanSettingService {
    private final static Logger logger = LoggerFactory.getLogger(PlanSettingService.class);
    @Autowired
    private PlanSettingMapper planSettingMapper;
    @Autowired
    private PlanMapper planMapper;
    @Autowired
    private PlanService planService;
    @Autowired
    private AppService appService;
    @Autowired
    private TaobaoApiShopService taobaoApiShopService;

    /**
     * 检测是否分类已经被其他计划使用
     *
     * @param userId
     * @param sellerCatStr
     * @return
     */
    public boolean isSellerCatUsed(Long userId, String sellerCatStr) {
        final List<Long> usedSellerCatIds = getUsedSellerCatIds(userId);
        List<Long> planSellerCatIds = Collections3.getLongList(sellerCatStr);
        return Iterables.any(planSellerCatIds, new Predicate<Long>() {
            @Override
            public boolean apply(@Nullable Long input) {
                return usedSellerCatIds.contains(input);
            }
        });
    }

    /**
     * 创建计划设置，需要检测是否所选择类目已经存在于其他计划了，如果存在则创建出错。
     *
     * @param planSetting
     */
    public void createPlanSetting(Long userId, PlanSetting planSetting) throws ShelfException, TaobaoSessionExpiredException, TaobaoAccessControlException, TaobaoEnhancedApiException {
        if (isSellerCatUsed(userId, planSetting.getIncludeSellerCids())) {
            throw new ShelfException("userId[" + userId + "] sellerCat[" + planSetting.getIncludeSellerCids() + "] is used");
        }
        planSetting.setUserId(userId);
        planSetting.setStatus(PLAN_SETTING_STATUS_WAITING_PLAN);
        planSetting.setDistributionType(PLAN_SETTING_DISTRIBUTE_TYPE_AUTO);
        planSetting.setDistribution(PlanDistributionUtils.getDefaultTimeDistributionJson());
        try {
            String hanYuPinyin = Languages.getHanYuPinyin(planSetting.getName());
            planSetting.setNamePinyin(hanYuPinyin);
        } catch (BadHanyuPinyinOutputFormatCombination badHanyuPinyinOutputFormatCombination) {
            logger.error("create createPlan setting error, planSetting[" + ToStringBuilder.reflectionToString(planSetting) + "].",
                    badHanyuPinyinOutputFormatCombination);
        }
        planSetting.setAddTime(appService.getLocalSystemTime().toDate());
        planSettingMapper.insertSelective(planSetting);
        planService.createPlan(userId, planSetting);
    }

    /**
     * 翻页获取用户计划设置
     *
     * @param userId
     * @return
     */
    public Page<PlanSetting> paginatePlanSettings(Long userId, int pageNum, int pageSize) {
        List<Byte> statusList = ImmutableList.of(PLAN_SETTING_STATUS_RUNNING, PLAN_SETTING_STATUS_WAITING_PLAN, PLAN_SETTING_STATUS_PAUSED);
        int totalSize = planSettingMapper.countByUserIdAndStatus(userId, statusList);
        List<PlanSetting> planSettings = planSettingMapper.paginateByUserIdAndStatus(userId, (pageNum - 1) * pageSize,
                pageSize, statusList);
        if (CollectionUtils.isEmpty(planSettings)) {
            return Page.create(totalSize, pageNum, pageSize, Lists.<PlanSetting>newArrayList());
        } else {
            //处理额外字段（总共需要调整宝贝数量，新加入宝贝数量，待调整宝贝数量）
            for (PlanSetting planSetting : planSettings) {
                PlanStatus planStatus = planMapper.calcPlanStatus(planSetting.getId(), planSetting.getLastPlanTime());
                if (null != planStatus) {
                    planSetting.setItemNum(planStatus.getItemNum());
                    planSetting.setNewItemNum(planStatus.getNewItemNum());
                    planSetting.setWaitAdjustItemNum(planStatus.getWaitAdjustItemNum());
                } else {
                    planSetting.setItemNum(0);
                    planSetting.setNewItemNum(0);
                    planSetting.setWaitAdjustItemNum(0);
                }
            }
            return Page.create(totalSize, pageNum, pageSize, planSettings);
        }
    }

    /**
     * @param planSetting
     * @throws ShelfException
     */
    @Transactional
    public void updatePlanSetting(Long userId, PlanSetting planSetting) throws ShelfException,
            TaobaoSessionExpiredException, TaobaoEnhancedApiException, TaobaoAccessControlException {
        planSetting.setUserId(userId);
        planSettingMapper.updateByPrimaryKeyAndUserIdSelective(planSetting);
        planMapper.deleteByUserIdAndPlanSettingId(userId, planSetting.getId());
        planService.updatePlan(userId,planSetting.getId());
    }

    /**
     * 只更新名字
     *
     * @param userId
     * @param planSettingId
     * @param name
     * @throws ShelfException
     */
    public void updatePlanSettingName(Long userId, Long planSettingId, String name) throws ShelfException {
        PlanSetting planSetting = new PlanSetting();
        planSetting.setId(planSettingId);
        planSetting.setName(name);
        planSetting.setUserId(userId);
        try {
            String hanYuPinyin = Languages.getHanYuPinyin(planSetting.getName());
            planSetting.setNamePinyin(hanYuPinyin);
        } catch (BadHanyuPinyinOutputFormatCombination badHanyuPinyinOutputFormatCombination) {
            logger.error("create createPlan setting error, planSetting[" + ToStringBuilder.reflectionToString(planSetting) + "].",
                    badHanyuPinyinOutputFormatCombination);
        }
        planSettingMapper.updateByPrimaryKeyAndUserIdSelective(planSetting);
    }

    public void updatePlanSettingDistribution(Long userId,
                                              Long planSettingId,
                                              Map<String, Map<String, Boolean>> distribution) throws ShelfException,
            TaobaoSessionExpiredException, TaobaoEnhancedApiException, TaobaoAccessControlException {
        PlanSetting planSetting = new PlanSetting();
        planSetting.setId(planSettingId);
        planSetting.setUserId(userId);
        planSetting.setDistribution(JsonMapper.nonEmptyMapper().toJson(distribution));
        planSetting.setDistributionType(PLAN_SETTING_DISTRIBUTE_TYPE_MANUAL);
        planSettingMapper.updateByPrimaryKeyAndUserIdSelective(planSetting);
        planSetting=planSettingMapper.selectByPrimaryKeyAndUserId(planSettingId,userId);
        planMapper.deleteByUserIdAndPlanSettingId(userId, planSetting.getId());
        planService.createPlan(userId,planSetting);
    }

    /**
     * 获取计划设置
     *
     * @param planSettingId
     * @return
     */
    public PlanSetting getPlanSetting(Long userId, Long planSettingId) {
        PlanSetting planSetting = planSettingMapper.selectByPrimaryKeyAndUserId(planSettingId, userId);
        if (PLAN_SETTING_DISTRIBUTE_TYPE_AUTO == planSetting.getDistributionType()) {
            planSetting.setDistribution(PlanDistributionUtils.getDefaultTimeDistributionJson());
        }
        return planSetting;
    }

    /**
     * 删除计划设置
     *
     * @param planSettingId
     * @return
     */
    @Transactional
    public boolean deletePlanSetting(Long userId, Long planSettingId) {
        int rows = planSettingMapper.deleteByPrimaryKeyAndUserId(planSettingId, userId);
        planService.deletePlan(userId, planSettingId);
        return rows > 0;
    }

    public boolean pausePlanSetting(Long userId, Long planSettingId) {
        PlanSetting planSetting = new PlanSetting();
        planSetting.setUserId(userId);
        planSetting.setId(planSettingId);
        planSetting.setStatus(ShelfConstants.PLAN_SETTING_STATUS_PAUSED);
        int rows = planSettingMapper.updateByPrimaryKeyAndUserIdSelective(planSetting);
        return rows > 0;
    }

    public boolean resumePlanSetting(Long userId, Long planSettingId) {
        PlanSetting planSetting = new PlanSetting();
        planSetting.setUserId(userId);
        planSetting.setId(planSettingId);
        planSetting.setStatus(ShelfConstants.PLAN_SETTING_STATUS_RUNNING);
        int rows = planSettingMapper.updateByPrimaryKeyAndUserIdSelective(planSetting);
        return rows > 0;
    }

    public Page<PlanSetting> searchPlanSettings(Long userId, String query, int pageNum, int pageSize) {
        int totalSize = planSettingMapper.countByUserIdAndName(userId, query);
        List<PlanSetting> planSettings = planSettingMapper.paginateByUserIdAndName(userId,
                query, (pageNum - 1) * pageSize,
                pageSize);
        if (CollectionUtils.isEmpty(planSettings)) {
            return Page.create(totalSize, pageNum, pageSize, Lists.<PlanSetting>newArrayList());
        } else {
            return Page.create(totalSize, pageNum, pageSize, planSettings);
        }
    }

    public Page<Plan> paginatePlans(Long userId, Long planSettingId, String query, int pageNum, int pageSize) {
        List<Byte> statusList = ImmutableList.of(PLAN_STATUS_SUCCESSFUL, PLAN_STATUS_WAITING_ADJUST, PLAN_STATUS_EXCLUDED);
        int totalSize = planMapper.countByUserIdAndPlanSettingIdAndStatus(userId, planSettingId, statusList, query);
        List<Plan> plans = planMapper.paginateByUserIdAndPlanSettingIdAndStatus(userId,
                planSettingId, statusList, query, (pageNum - 1) * pageSize,
                pageSize);
        if (CollectionUtils.isEmpty(plans)) {
            return Page.create(totalSize, pageNum, pageSize, Lists.<Plan>newArrayList());
        } else {
            return Page.create(totalSize, pageNum, pageSize, plans);
        }
    }

    /**
     * 获取已经在计划中的的宝贝分类
     *
     * @param userId
     * @return 不会返回 null，如果结果为空，返回一个空的{@link Set}
     */
    @NotNull
    public List<Long> getUsedSellerCatIds(Long userId) {
        Set<Long> plannedSellerCatIds = Sets.newHashSet();
        int pageNum = 1;
        while (true) {
            Page<PlanSetting> PlanSettingPage = paginatePlanSettings(userId, pageNum, 100);

            for (PlanSetting planSetting : PlanSettingPage.getItems()) {
                plannedSellerCatIds.addAll(Collections3.getLongList(planSetting.getIncludeSellerCids()));
            }
            if (PlanSettingPage.isLastPage()) {
                break;
            } else {
                pageNum++;
            }
        }
        return Lists.newArrayList(plannedSellerCatIds);
    }

    /**
     * 获取当前宝贝的 dayOfWeek 分布情况 TODO cache
     *
     * @param userId
     * @return
     * @throws TaobaoEnhancedApiException
     * @throws TaobaoSessionExpiredException
     */
    public ShelfStatus getShelfStatus(Long userId) throws TaobaoEnhancedApiException, TaobaoSessionExpiredException, TaobaoAccessControlException {
        //获取当前实际的宝贝在dayOfWeek的分布情况
        ItemsOnsaleGetRequest request = new ItemsOnsaleGetRequest();
        request.setFields("delist_time");
        ItemsOnsaleGetResponse result = taobaoApiShopService.getOnSaleItems(userId, request);
        List<Integer> listItemNum = Lists.newArrayList();
        Multiset<Integer> dayOfWeekNum = ShelfUtils.getItemDelistDayOfWeekNum(result.getItems());
        ShelfStatus shelfStatus = new ShelfStatus();
        shelfStatus.setDayOfWeek(Lists.newArrayList(1, 2, 3, 4, 5, 6, 7));
        listItemNum.add(dayOfWeekNum.count(1));
        listItemNum.add(dayOfWeekNum.count(2));
        listItemNum.add(dayOfWeekNum.count(3));
        listItemNum.add(dayOfWeekNum.count(4));
        listItemNum.add(dayOfWeekNum.count(5));
        listItemNum.add(dayOfWeekNum.count(6));
        listItemNum.add(dayOfWeekNum.count(7));
        shelfStatus.setListItemNum(listItemNum);
        return shelfStatus;
    }

    public List<SellerCatExtended> getSellerCatsExtended(Long userId) throws TaobaoSessionExpiredException,
            TaobaoAccessControlException, TaobaoEnhancedApiException {
        List<Long> plannedSellerCatIds = getUsedSellerCatIds(userId);
        return taobaoApiShopService.getOnsaleSellerCatExtended(userId, plannedSellerCatIds);
    }

    public Plan getPlan(Long userId,Long numIid){
       return  planMapper.selectByUserIdAndNumIid(userId,numIid);
    }
}
