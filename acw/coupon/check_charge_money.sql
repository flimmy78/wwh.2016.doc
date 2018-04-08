DROP PROCEDURE check_charge_money;
CREATE PROCEDURE check_charge_money(pkEpGunId int, totalPower DECIMAL(8,3), chargeAmt DECIMAL(10,4), serviceAmt DECIMAL(10,4)
    , beginTime int, endTime int, out totalAmt DECIMAL(10,4), out rtn int)
BEGIN
    DECLARE maxChargeAmt INT;
    DECLARE maxServiceAmt INT;
    DECLARE maxChargeCost INT;
    DECLARE chargeTime INT;

    set rtn = 0;
    set maxChargeAmt = 500;
    set maxServiceAmt = 500;
    set maxChargeCost = maxChargeAmt + maxServiceAmt;
    set chargeTime = ceiling((endTime - beginTime)/60);
    if (chargeTime < 0 or chargeTime > 1440) then
        if (rtn = 0) then
            set rtn = -1;
        end if;
    end if;
    if (totalPower < 0 or totalPower > 900) then
        if (rtn = 0) then
            set rtn = -2;
        end if;
    end if;
    if (chargeAmt < 0 or chargeAmt > maxChargeAmt) then
        if (rtn = 0) then
            set rtn = -3;
        end if;
    end if;
    if (serviceAmt < 0 or serviceAmt > maxServiceAmt) then
        if (rtn = 0) then
            set rtn = -4;
        end if;
    end if;
    set totalAmt = chargeAmt + serviceAmt;
    if (totalAmt < 0 or totalAmt > maxChargeCost) then
        if (rtn = 0) then
            set rtn = -5;
        end if;
    end if;

    update tbl_electricpilehead
    set
        total_charge_dl = total_charge_dl + (case rtn when -2 then 0 else totalPower end),
        total_charge_time = total_charge_time + (case rtn when -1 then 0 else chargeTime end),
        total_charge_amt = total_charge_amt + (case when rtn < -2 then 0 else totalAmt end)
    where 
        pk_ElectricpileHead=pkEpGunId;
END