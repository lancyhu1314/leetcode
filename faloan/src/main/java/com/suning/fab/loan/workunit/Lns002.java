package com.suning.fab.loan.workunit;

import com.alibaba.fastjson.JSONObject;
import com.suning.fab.loan.domain.TblStatutorydays;
import com.suning.fab.loan.supporter.RepayFormulaSupporter;
import com.suning.fab.loan.utils.ConstantDeclare;
import com.suning.fab.loan.utils.LoanDbUtil;
import com.suning.fab.tup4j.base.WorkUnit;
import com.suning.fab.tup4j.utils.DbAccessUtil;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 〈一句话功能简述〉
 * 〈功能详细描述〉：维护工作日
 *
 * @Author 18049705 MYP
 * @Date Created in 16:29 2020/2/17
 * @see
 */
@Scope("prototype")
@Repository
public class Lns002 extends WorkUnit {


    //周末改工作日
    private  String   workdays;
    //工作日改假日
    private  String   holidays;

    @Override
    public void run() throws Exception {
//        Map<String,Object> param = new HashMap<>();
//        param.put("date", Date.valueOf(tranctx.getTranDate()));
//        //备份
//        LOANDbUtil.update("Statutorydays.reserve",param);
//        //删除
//        LOANDbUtil.delete("Statutorydays.delete",param);


        List<Map<String, Object>> insertMaps = new ArrayList<>();
        //插入新数据
        if(workdays!=null){
            List<String> list = JSONObject.parseArray(workdays,String.class);
            for (String workday : list)
            {
                Map<String, Object> insert = new HashMap<>();
                insert.put("type", ConstantDeclare.DATETYPE.WORKDAY);
                insert.put("date",workday);
                insertMaps.add(insert);
            }

        }
        //插入新数据
        if(holidays!=null){
            List<String> list = JSONObject.parseArray(holidays,String.class);
            for (String holiday : list)
            {
                Map<String, Object> insert = new HashMap<>();
                insert.put("type",ConstantDeclare.DATETYPE.HOLIDAY);
                insert.put("date",holiday);
                insertMaps.add(insert);
            }

        }
        LoanDbUtil.batchUpdate("Statutorydays.insert",insertMaps.toArray(new Map[insertMaps.size()]) );

        //更新内存，运行中的数据

        List<TblStatutorydays>  tblStatutorydays = DbAccessUtil.queryForList("Statutorydays.query", new HashMap<>(), TblStatutorydays.class);
        List<String> workDays = new ArrayList<>();
        List<String> holiDays = new ArrayList<>();

        if(tblStatutorydays!=null) {
            for (TblStatutorydays statutoryday : tblStatutorydays) {
                if (ConstantDeclare.DATETYPE.WORKDAY.equals(statutoryday.getType())) {
                    workDays.add(statutoryday.getDate().toString());
                } else {
                    holiDays.add(statutoryday.getDate().toString());
                }

            }
        }
//        RepayFormulaSupporter.setWorkDays(workDays);
//        RepayFormulaSupporter.setHolidays(holiDays);
    }

    /**
     * Gets the value of workdays.
     *
     * @return the value of workdays
     */
    public String getWorkdays() {
        return workdays;
    }

    /**
     * Sets the workDays.
     *
     * @param workdays workdays
     */
    public void setWorkDays(String workdays) {
        this.workdays = workdays;

    }

    /**
     * Gets the value of holidays.
     *
     * @return the value of holidays
     */
    public String getHolidays() {
        return holidays;
    }

    /**
     * Sets the holidays.
     *
     * @param holidays holidays
     */
    public void setHolidays(String holidays) {
        this.holidays = holidays;

    }
}
