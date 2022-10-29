package com.xd.pre.modules.sys.mapper;

import cn.hutool.core.date.DateTime;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xd.pre.modules.sys.domain.JdMchOrder;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface JdMchOrderMapper extends BaseMapper<JdMchOrder> {

    @Select("select ifnull(sum(money),0) from  jd_mch_order  " +
            "where  create_time BETWEEN  #{startTime} and #{endTime}  ")
    BigDecimal selectTotalFlowingWater(@Param("startTime") String startTime, @Param("endTime") String endTime);

    @Select("select ifnull(sum(money),0) from  jd_mch_order  " +
            "where  create_time BETWEEN  #{startTime} and #{endTime}  " +
            "and status=2")
    BigDecimal selectSuccessFlowingWater(@Param("startTime") String startTime, @Param("endTime") String endTime);

    @Select("select ifnull(sum(money),0) from  jd_mch_order  " +
            "where  create_time BETWEEN  #{startTime} and #{endTime}  " +
            "and (status=1  or status=0 ) and (original_trade_no is  null or original_trade_no ='')")
    BigDecimal selectFailFlowingWater(@Param("startTime") String startTime, @Param("endTime") String endTime);

    @Select("select ifnull(sum(money),0) from  jd_mch_order  " +
            " where  create_time BETWEEN  #{startTime} and #{endTime}  " +
            "  and (original_trade_no is  null  or original_trade_no ='')")
    BigDecimal selectNoMatchFlowingWater(@Param("startTime") String startTime, @Param("endTime") String endTime);

    @Select("update jd_mch_order set `original_trade_no` = null , `match_time`= null  ,   `status`  = 0  where id =#{xxx} ")
    void updateTradeNoById(@Param("xxx") Integer id);

    @Select("update  jd_mch_order  set notify_succ =#{one}    where id  =#{id}")
    void updateByIdNotSuccess(@Param("id") Integer id, @Param("one") Integer one);


    @Select("SELECT " +
            " count(1) as count,lo.ip as ip  ,mo.tenant_id  as tenant_id " +
            "FROM " +
            " jd_mch_order mo " +
            " LEFT JOIN jd_log lo ON lo.order_id = mo.trade_no  " +
            "WHERE " +
            " mo.create_time > #{beginOfDay} " +
            " and mo.`status` !=2 and mo.match_time<=10 " +
            " GROUP BY lo.ip,mo.tenant_id " +
            " HAVING   count(1)>5")
    List<Map<String, Object>> selectBlackData(@Param("beginOfDay") DateTime beginOfDay);

    @Select("SELECT " +
            "count(1) " +
            "FROM " +
            " jd_mch_order mo " +
            " LEFT JOIN jd_log lo ON lo.order_id = mo.trade_no  " +
            "WHERE " +
            " mo.create_time > #{beginOfDay} " +
            " and mo.status =2 and lo.ip = #{ip}")
    Integer selectBlackDataByIp(@Param("beginOfDay") DateTime beginOfDay, @Param("ip") String ip);

    @Select("SELECT " +
            " mo.* " +
            " FROM " +
            " jd_mch_order mo " +
            " LEFT JOIN jd_order_pt op ON op.id = mo.original_trade_id  " +
            " WHERE " +
            " mo.create_time > DATE_SUB( SYSDATE( ), INTERVAL 100 MINUTE )  " +
            " and mo.create_time < DATE_SUB( SYSDATE( ), INTERVAL  5  MINUTE )  " +
            " and mo.original_trade_id is not null  and mo.click_pay is not null  " +
            " and mo.click_pay !='1970-01-01 08:00:00' " +
            " AND op.html IS NULL " +
            " and mo.`status`!=2;")
    List<JdMchOrder> selectBuDan(@Param("time") Integer time);

    @Select("SELECT " +
            " mo.*" +
            "FROM " +
            " jd_mch_order mo " +
            " LEFT JOIN jd_order_pt op ON op.id = mo.original_trade_id  " +
            "WHERE " +
            " mo.create_time > DATE_SUB( SYSDATE( ), INTERVAL 100 MINUTE )  " +
            " AND mo.create_time < DATE_SUB( SYSDATE( ), INTERVAL 20 MINUTE )  " +
            " AND mo.click_pay IS NOT NULL  " +
            " AND mo.click_pay != '1970-01-01 08:00:00'  " +
            " AND timestampdiff( MINUTE, mo.click_pay, op.org_app_ck ) < 15  " +
            " AND mo.`status` != 2;")
    List<JdMchOrder> selectbudanData15();

    @Select("SELECT " +
            " mo.*" +
            "FROM " +
            " jd_mch_order mo " +
            " LEFT JOIN jd_order_pt op ON op.id = mo.original_trade_id  " +
            "WHERE " +
            " mo.create_time > DATE_SUB( SYSDATE( ), INTERVAL 100 MINUTE )  " +
            " AND mo.create_time < DATE_SUB( SYSDATE( ), INTERVAL 14 MINUTE )  " +
            " AND mo.click_pay IS NOT NULL  " +
            " AND mo.click_pay != '1970-01-01 08:00:00'  " +
            " AND timestampdiff( MINUTE, mo.click_pay, op.org_app_ck ) < 10  " +
            " AND mo.`status` != 2")
    List<JdMchOrder> selectbudanData10();


}
