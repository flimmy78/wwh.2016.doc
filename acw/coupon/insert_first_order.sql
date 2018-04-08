DROP PROCEDURE insert_first_order;
CREATE PROCEDURE insert_first_order(pkEpGunId int, pkConsumeRecord int, rateId int,currentType int, type int, out rtn int)
top:BEGIN
    DECLARE transactionNumber VARCHAR(40);
    DECLARE epCode VARCHAR(40);
    DECLARE epGunNo INT;
    DECLARE accountType INT;
    DECLARE userOrigin INT;
    DECLARE userId INT;
    DECLARE userAccount VARCHAR(40);
    DECLARE pkCardId INT;
    DECLARE account VARCHAR(40);
    DECLARE userLevel INT;
    DECLARE tipPower DECIMAL(8,3);
    DECLARE peakPower DECIMAL(8,3);
    DECLARE usualPower DECIMAL(8,3);
    DECLARE valleyPower DECIMAL(8,3);
    DECLARE totalPower DECIMAL(8,3);
    DECLARE chargeAmt DECIMAL(10,4);
    DECLARE serviceAmt DECIMAL(10,4);
    DECLARE totalAmt DECIMAL(10,4);
    DECLARE startSoc INT;
    DECLARE endSoc INT;
    DECLARE startMeterNum DECIMAL(8,3);
    DECLARE endMeterNum DECIMAL(8,3);
    DECLARE transType INT;
    DECLARE beginTime INT;
    DECLARE endTime INT;
    DECLARE orderNo VARCHAR(40);
    DECLARE orderStatus INT DEFAULT -1;
    DECLARE quantumDate VARCHAR(20);
    DECLARE tipPrice DECIMAL(8,4);
    DECLARE peakPrice DECIMAL(8,4);
    DECLARE usualPrice DECIMAL(8,4);
    DECLARE valleyPrice DECIMAL(8,4);
    DECLARE serviceCharge DECIMAL(10,4);
    DECLARE stopCause INT;

    DECLARE status INT;
    DECLARE frozenAmt DECIMAL(10,4) DEFAULT 0;
    DECLARE payMode INT;
    DECLARE expFlag INT;
    DECLARE checkRtn INT;
    DECLARE remainAmt DECIMAL(10,4);

    DECLARE chargingMethod INT;
    DECLARE carVinCode VARCHAR(40);
    DECLARE pkCouPon INT DEFAULT 0;
    DECLARE couponAmt DECIMAL(10,4) DEFAULT 0;
    DECLARE realCouponAmt DECIMAL(10,4);
    DECLARE token VARCHAR(40);
    DECLARE thirdType INT DEFAULT 0;

    SELECT transaction_number,ep_code,ep_gun_no,account_type,user_origin,user_account,trans_type,begin_charge_time,end_charge_time
        ,tip_power/1000,peak_power/1000,usual_power/1000,valley_power/1000,total_power/1000,start_meter_num/1000,end_meter_num/1000
        ,if(type=0,charge_money/100,charge_money/10000),if(type=0,service_money/100,service_money/10000),car_vin_code,start_soc,end_soc,stop_cause
    INTO transactionNumber,epCode,epGunNo,accountType,userOrigin,userAccount,transType,beginTime,endTime
        ,tipPower,peakPower,usualPower,valleyPower,totalPower,startMeterNum,endMeterNum
        ,chargeAmt,serviceAmt,carVinCode,startSoc,endSoc,stopCause
    FROM tbl_consume_record
    WHERE pk_consume_record = pkConsumeRecord;

    if (accountType = 3 or accountType = 4) then
        if (userOrigin = 1002) then
            set account = '13879903894';
            SELECT e.user_account, 3
            INTO account,userLevel
            FROM tbl_user e
            LEFT JOIN tbl_user_normal r
            ON r.user_id = e.user_id
            WHERE e.user_account = account;
            if isnull(userLevel) then
                set rtn = 4;
            end if;
        else
            SELECT r.user_id
            INTO userId
            FROM tbl_company e
            LEFT JOIN tbl_user_company r
            ON e.cpy_id = r.cpy_id
            WHERE e.cpy_number = userOrigin;
            if isnull(userId) then
                set rtn = 3;
            end if;

            SELECT pk_UserCard
            INTO pkCardId
            FROM tbl_usercard
            WHERE uc_InternalCardNumber = userAccount;
            if isnull(pkCardId) then
                if (userOrigin = 1008 or userOrigin = 9003) then
                    insert into tbl_usercard (
                        uc_InternalCardNumber,
                        uc_ExternalCardNumber,
                        uc_Balance,
                        uc_CompanyNumber,
                        uc_UserId,
                        uc_Status,
                        uc_pay_mode)
                    values (
                        userAccount,
                        userAccount,
                        0,
                        userOrigin,
                        userId,
                        0,
                        12);select @@IDENTITY into pkCardId;
                else
                    set rtn = 2;
                end if;
            end if;
            SELECT e.user_account,case e.user_leval when 6 then 1 when 5 then 2 else 3 end
            INTO account,userLevel
            FROM tbl_user e
            LEFT JOIN tbl_user_normal r
            ON r.user_id = e.user_id
            WHERE e.user_id = userId;
            if isnull(userLevel) then
                set rtn = 4;
            end if;
        end if;
        set payMode = 2;
    else
        SELECT chRe_ChargingMethod,user_phone,user_id,chRe_Status,chRe_FrozenAmt,pk_UserCard,chRe_PayMode
            ,chRe_Code,chRe_ServiceCharge,chre_parter_extradata
        INTO chargingMethod,account,userId,status,frozenAmt,pkCardId,payMode,orderNo,serviceCharge,token
        FROM tbl_ChargingRecord
        WHERE chRe_TransactionNumber = transactionNumber;
        if isnull(userId) then
            set rtn = -5;
            leave top;
        end if;
        if (status = 1 or status = 3 or status = 7) then
            set rtn = 3;
        end if;
        SELECT case e.user_leval when 6 then 1 when 5 then 2 else 3 end
        INTO userLevel
        FROM tbl_user e
        LEFT JOIN tbl_user_normal r
        ON r.user_id = e.user_id
        WHERE e.user_id = userId;
        if isnull(userLevel) then
            set rtn = 4;
        end if;
        if (stopCause > 2 and stopCause !=10) then
            insert into tbl_ChargingFaultRecord (
                cFRe_UsingMachineCode,
                cFRe_EphNo,
                cFRe_ElectricPileID,
                cFRe_ElectricPileName,
                cFRe_ChargingSarttime,
                cFRe_FaultCause,
                cFRe_Createdate,
                cFRe_Updatedate,
                cFRe_TransactionNumber
            )values (
                epCode,
                epGunNo,
                pkEpGunId,
                '',
                FROM_UNIXTIME(endTime),
                stopCause,
                SYSDATE(),
                SYSDATE(),
                transactionNumber
            );
        end if;
    end if;

    if (frozenAmt > 0 and frozenAmt < chargeAmt + serviceAmt) then
        set serviceAmt = frozenAmt - chargeAmt;
        if (serviceAmt < 0) then
            set serviceAmt = 0;
            set chargeAmt = frozenAmt;
        end if;
    end if;
    call check_charge_money(pkEpGunId, totalPower, chargeAmt, serviceAmt
    , beginTime, endTime, totalAmt, checkRtn);
    if (checkRtn = 0) then
        set expFlag = 0;
    else
        set expFlag = 1;
    end if;
    if (!isnull(carVinCode) and carVinCode!='') then
         call p_vincode_stage(transactionNumber, epCode, userId, carVinCode, frozenAmt, payMode, type
             , totalPower, chargeAmt,serviceAmt, totalAmt, couponAmt, pkCoupon);
         if !isnull(pkCoupon) then
             set thirdType = 2;
         end if;
    else
        if (accountType != 3 and accountType != 4) then
            call p_coupon_stage(transactionNumber, epCode, userId, totalAmt, currentType
                , couponAmt, realCouponAmt, pkCoupon);
            if !isnull(pkCoupon) then
                set thirdType = 1;
            end if;
        end if;
    end if;

    set remainAmt = frozenAmt - totalAmt;
    UPDATE tbl_user_normal SET
        norm_account_balance = norm_account_balance + remainAmt
    WHERE 
        user_id=userId;

    insert into tbl_purchasehistory (
        puHi_Type,
        puHi_PurchaseHistoryTime,
        puHi_Monetary,
        puHi_ConsumerRemark,
        puHi_PurchaseContent,
        puHi_Createdate,
        puHi_UserId,
        puHi_UserOrigin,
        puHi_ElectricPileCode,
        puHi_TransactionNumber
    ) values (
        1,
        current_date,
        totalAmt,
        '',
        '³äµçÏû·Ñ',
        SYSDATE(),
        userId,
        0,
        epCode,
        transactionNumber
    );

    call p_merge_order(pkEpGunId, transactionNumber, epCode, epGunNo, userId,userLevel, tipPower, peakPower, usualPower, valleyPower, totalPower
    , transType, beginTime, endTime, account, chargeAmt, serviceAmt, pkCardId, userOrigin, userAccount, token, pkCouPon, couPonAmt, thirdType
    , startSoc,endSoc,startMeterNum, endMeterNum, chargingMethod, payMode, serviceCharge, rateId, expFlag);

    set rtn = 1;
END