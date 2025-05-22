package com.suning.fab.loan.utils;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.suning.fab.loan.bo.LnsBill;
import com.suning.fab.loan.domain.TblLnsbill;
import com.suning.fab.tup4j.utils.PropertyUtil;

public class LoanRepayOrderHelper {

	private LoanRepayOrderHelper() {
		//nothing to do
	}

	/**
	 * 小本小息
	 *
	 * @param billList
	 * @return
	 */
	public static List<TblLnsbill> smallPrinSmallInt(List<TblLnsbill> billList) {
		List<TblLnsbill> bt = billList;
		Collections.sort(bt,
				new Comparator<TblLnsbill>() {
					@Override
					public int compare(TblLnsbill o1, TblLnsbill o2) {
						Integer p1 = o1.getPeriod() * 1000 + Integer.parseInt(PropertyUtil
								.getPropertyOrDefault(
										"repayorder." + o1.getBilltype(),
										"1000"));
						Integer p2 = o2.getPeriod() * 1000 + Integer.parseInt(PropertyUtil
								.getPropertyOrDefault(
										"repayorder." + o2.getBilltype(),
										"1000"));
						return p1.compareTo(p2);
					}


				});
		return bt;
	}

	/**
	 * 大本大息
	 *
	 * @param billList
	 * @return
	 */
	public static List<TblLnsbill> bigPrinBigInt(List<TblLnsbill> billList) {
		List<TblLnsbill> bt = billList;
		Collections.sort(bt,
				new Comparator<TblLnsbill>() {
					@Override
					public int compare(TblLnsbill o1, TblLnsbill o2) {
						Integer p1 = Integer.parseInt(PropertyUtil
								.getPropertyOrDefault(
										"repayorder." + o1.getBilltype(),
										"1000")) * 1000 + o1.getPeriod();
						Integer p2 = Integer.parseInt(PropertyUtil
								.getPropertyOrDefault(
										"repayorder." + o2.getBilltype(),
										"1000")) * 1000 + o2.getPeriod();
						return p1.compareTo(p2);
					}


				});
		return bt;
	}

	/**
	 * 呆滞呆账
	 *
	 * @param billList
	 * @return
	 */
	public static List<TblLnsbill> dullBad(List<TblLnsbill> billList) {
		List<TblLnsbill> bt = billList;
		Collections.sort(bt,
				new Comparator<TblLnsbill>() {
					@Override
					public int compare(TblLnsbill o1, TblLnsbill o2) {
						Integer p1 = Integer.parseInt(PropertyUtil
								.getPropertyOrDefault(
										"repayorder." + o1.getBilltype(),
										"1000")) * 1000 + 1000 - o1.getPeriod();
						Integer p2 = Integer.parseInt(PropertyUtil
								.getPropertyOrDefault(
										"repayorder." + o2.getBilltype(),
										"1000")) * 1000 + 1000 - o2.getPeriod();
						return p2.compareTo(p1);
					}


				});
		return bt;
	}
	/**
	 * 未呆滞呆账账本
	 * 费用 违约金 本息罚 还款时顺序
	 * 优先级从大到小依次是  违约金>一次性费用>分期费用>本金>利息>罚息
	 * @param billList 未排序账本集合
	 * @return billList 排序后账本集合
	 */
	public static void allNormalSort(List<TblLnsbill> billList) {

		billList.sort(
				new Comparator<TblLnsbill>() {
					@Override
					public int compare(TblLnsbill o1, TblLnsbill o2) {
						// 期数优先（期数越大优先级越低） 账本类型其次
						Integer p1 = o1.getPeriod() * 1000 + normalBillTypePriority(o1.getBilltype(),o1.getRepayway());
						Integer p2 = o2.getPeriod() * 1000 + normalBillTypePriority(o2.getBilltype(),o2.getRepayway());
						//小的优先
						return p1.compareTo(p2);
					}
				});

	}
	public static void allNormalSortBill(List<LnsBill> billList) {

		billList.sort(
				new Comparator<LnsBill>() {
					@Override
					public int compare(LnsBill o1, LnsBill o2) {
						// 期数优先（期数越大优先级越低） 账本类型其次
						Integer p1 = o1.getPeriod() * 1000 + normalBillTypePriority(o1.getBillType(),o1.getRepayWay());
						Integer p2 = o2.getPeriod() * 1000 + normalBillTypePriority(o2.getBillType(),o2.getRepayWay());
						//小的优先
						return p1.compareTo(p2);
					}
				});

	}

	/**
	 * 呆滞呆账的排序顺序
	 * 费用 违约金 本息罚 还款时顺序
	 * 优先级从大到小依次是  违约金>一次性费用>分期费用>本金>利息>罚息
	 * @param billList 未排序账本集合
	 * @return billList 排序后账本集合
	 */
	public static void allBadSort(List<TblLnsbill> billList) {

		billList.sort(
				new Comparator<TblLnsbill>() {
					@Override
					public int compare(TblLnsbill o1, TblLnsbill o2) {
						//账本类型优先 期数其次（期数越大优先级越低）
						Integer p1 = badBillTypePriority(o1.getBilltype(),o1.getRepayway()) * 1000 + 1000 - o1.getPeriod();
						Integer p2 = badBillTypePriority(o2.getBilltype(),o2.getRepayway()) * 1000 + 1000 - o2.getPeriod();
						//大的优先
						return p2.compareTo(p1);
					}
				});

	}
	public static void allBadSortBill(List<LnsBill> billList) {

		billList.sort(
				new Comparator<LnsBill>() {
					@Override
					public int compare(LnsBill o1, LnsBill o2) {
						//账本类型优先 期数其次（期数越大优先级越低）
						Integer p1 = badBillTypePriority(o1.getBillType(),o1.getRepayWay()) * 1000 + 1000 - o1.getPeriod();
						Integer p2 = badBillTypePriority(o2.getBillType(),o2.getRepayWay()) * 1000 + 1000 - o2.getPeriod();
						//大的优先
						return p2.compareTo(p1);
					}
				});

	}
	//账本顺序  呆滞呆账 数字小优先级高
	private static Integer normalBillTypePriority(String billtype,String repayWay){

		//违约金
		if(LoanFeeUtils.isPenalty(billtype)){
			return -2;
		}
		//费用 一次性的优先
		if(LoanFeeUtils.isFeeType(billtype)){
			if(ConstantDeclare.FEEREPAYWAY.ONETIME.equals(repayWay))
				return -1;
			return 0;
		}
		//默认的本息罚顺序  0到5
		return Integer.parseInt(PropertyUtil
				.getPropertyOrDefault(
						"repayorder." +billtype,
						"1000"));
	}
	//账本顺序  呆滞呆账 数字大优先级高
	private static Integer badBillTypePriority(String billtype,String repayWay){

		//违约金
		if(LoanFeeUtils.isPenalty(billtype)){
			return 8;
		}
		//费用 一次性的优先
		if(LoanFeeUtils.isFeeType(billtype)){
			if(ConstantDeclare.FEEREPAYWAY.ONETIME.equals(repayWay))
				return 7;
			return 6;
		}
		//默认的本息罚顺序  0到5
		return Integer.parseInt(PropertyUtil
				.getPropertyOrDefault(
						"repayorder." +billtype,
						"1000"));
	}

	public static void dullBadFeeBills(List<LnsBill> billList) {
		Collections.sort(billList,
				new Comparator<LnsBill>() {
					@Override
					public int compare(LnsBill o1, LnsBill o2) {
						Integer p1 = Integer.parseInt(PropertyUtil
								.getPropertyOrDefault(
										"repayorder." + o1.getBillType(),
										"1000")) * 1000 + 1000 - o1.getPeriod();
						Integer p2 = Integer.parseInt(PropertyUtil
								.getPropertyOrDefault(
										"repayorder." + o2.getBillType(),
										"1000")) * 1000 + 1000 - o2.getPeriod();
						if(LoanFeeUtils.isFeeType(o1.getBillType()))
							p1+=10000;
						if(LoanFeeUtils.isFeeType(o2.getBillType()))
							p2+=10000;
						return p2.compareTo(p1);
					}


				});
	}
	/**
	 * 小本小息
	 *
	 * @param billList
	 * @return
	 */
	public static void dullNormalFeeBill (List<LnsBill> billList) {
		Collections.sort(billList,
				new Comparator<LnsBill>() {
					@Override
					public int compare(LnsBill o1, LnsBill o2) {
						Integer p1 = o1.getPeriod() * 1000 + Integer.parseInt(PropertyUtil
								.getPropertyOrDefault(
										"repayorder." + o1.getBillType(),
										"1000"))+o1.onetimeFee();
						Integer p2 = o2.getPeriod() * 1000 + Integer.parseInt(PropertyUtil
								.getPropertyOrDefault(
										"repayorder." + o2.getBillType(),
										"1000"))+o1.onetimeFee();
						return p1.compareTo(p2);
					}


				});
	}

}
