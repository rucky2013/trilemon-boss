package com.trilemon.boss360.shelf;

import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 * @author kevin
 */
public interface ShelfConstants {
    //interval
    byte PLAN_SETTING_DISTRIBUTE_TYPE_AUTO = 0;
    byte PLAN_SETTING_DISTRIBUTE_TYPE_MANUAL = 1;
    //status
    byte PLAN_SETTING_STATUS_WAITING_PLAN = 0;
    byte PLAN_SETTING_STATUS_RUNNING = 1;
    byte PLAN_SETTING_STATUS_PAUSE = 2;
    //plan status
    byte PLAN_STATUS_WAITING_ADJUST = 0;
    byte PLAN_STATUS_SUCCESSFUL = 1;
    byte PLAN_STATUS_FAILED = 2;
    //on sale item field
    //"num_iid", "delist_time","title","pic_url","seller_cids"
    List<String> ITEM_FIELDS = ImmutableList.of("num_iid", "delist_time", "title", "pic_url", "seller_cids");
}