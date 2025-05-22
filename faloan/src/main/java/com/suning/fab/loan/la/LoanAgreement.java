package com.suning.fab.loan.la;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

@Scope("prototype")
@Repository
public class LoanAgreement  extends Product implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 230831622469246916L;
	private  Contract   contract;
	private  Customer   customer;
	private  FundInvest   fundInvest;
	private  BasicExtension   basicExtension;
	
	public LoanAgreement(){
		contract=new Contract();
		customer=new Customer();
		fundInvest=new FundInvest();
		basicExtension = new BasicExtension();
	}
	public Object deepClone() throws IOException, ClassNotFoundException {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        ObjectOutputStream oo = new ObjectOutputStream(bo);
        oo.writeObject(this);
        ByteArrayInputStream bi = new ByteArrayInputStream(bo.toByteArray());
        ObjectInputStream oi = new ObjectInputStream(bi);
        return (oi.readObject());
    }
	/**
	 * @return the contract
	 */
	public Contract getContract() {
		return contract;
	}
	/**
	 * @param contract the contract to set
	 */
	public void setContract(Contract contract) {
		this.contract = contract;
	}
	/**
	 * @return the customer
	 */
	public Customer getCustomer() {
		return customer;
	}
	/**
	 * @param customer the customer to set
	 */
	public void setCustomer(Customer customer) {
		this.customer = customer;
	}
	/**
	 * @return the fundInvest
	 */
	public FundInvest getFundInvest() {
		return fundInvest;
	}
	/**
	 * @param fundInvest the fundInvest to set
	 */
	public void setFundInvest(FundInvest fundInvest) {
		this.fundInvest = fundInvest;
	}
	/**
	 * @return the serialversionuid
	 */
	public static long getSerialversionuid() {
		return serialVersionUID;
	}
	public BasicExtension getBasicExtension() {
		return basicExtension;
	}
	public void setBasicExtension(BasicExtension basicExtension) {
		this.basicExtension = basicExtension;
	}
}
