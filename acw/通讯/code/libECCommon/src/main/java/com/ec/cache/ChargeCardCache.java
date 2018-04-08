package com.ec.cache;


public class ChargeCardCache {

    private int id;
    private String cardInNo;
    private String cardOutNo;

    private int companyNumber;
    private int usrId;
    private int status;
    private int cardType;
    private int accountId;
    private boolean allowMutliCharge;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCardInNo() {
        return cardInNo;
    }

    public void setCardInNo(String cardInNo) {
        this.cardInNo = cardInNo;
    }

    public String getCardOutNo() {
        return cardOutNo;
    }

    public void setCardOutNo(String cardOutNo) {
        this.cardOutNo = cardOutNo;
    }

    public int getCompanyNumber() {
        return companyNumber;
    }

    public void setCompanyNumber(int companyNumber) {
        this.companyNumber = companyNumber;
    }

    public int getUserId() {
        return usrId;
    }

    public void setUserId(int userId) {
        this.usrId = userId;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getCardType() {
        return cardType;
    }

    public void setCardType(int cardType) {
        this.cardType = cardType;
    }

    public int getAccountId() {
        return accountId;
    }

    public void setAccountId(int accountId) {
        this.accountId = accountId;
    }

    public boolean isAllowMutliCharge() {
        return allowMutliCharge;
    }

    public void setAllowMutliCharge(boolean allowMutliCharge) {
        this.allowMutliCharge = allowMutliCharge;
    }

    public ChargeCardCache(int id, String cardInNo, String cardOutNo, int companyNumber,
                           int userId, int status, int payMod, int accountId, boolean allowMutliCharge) {
        this.id = id;
        this.cardInNo = cardInNo;
        this.cardOutNo = cardOutNo;

        this.companyNumber = companyNumber;
        this.usrId = userId;
        this.status = status;
        this.cardType = payMod;
        this.accountId = accountId;
        this.allowMutliCharge = allowMutliCharge;
    }

    @Override
    public String toString() {

        final StringBuilder sb = new StringBuilder();
        sb.append("ChargeCardCache");

        sb.append("{id=").append(id).append("}\n");
        sb.append(",{cardInNo=").append(cardInNo).append("}\n");
        sb.append(",{cardOutNo=").append(cardOutNo).append("}\n");
        sb.append(",{companyNumber=").append(companyNumber).append("}\n");
        sb.append(",{userId=").append(usrId).append("}\n");
        sb.append(",{status=").append(status).append("}\n");
        sb.append(",{payMode=").append(cardType).append("}\n");
        sb.append(",{allowMutliCharge=").append(allowMutliCharge).append("}\n");

        return sb.toString();
    }
}
