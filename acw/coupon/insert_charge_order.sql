DROP PROCEDURE insert_charge_order;
CREATE PROCEDURE insert_charge_order(pkEpGunId int, pkConsumeRecord int, rateId int, type int, out rtn int)
BEGIN
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
    DECLARE startMeterNum DECIMAL(8,3);
    DECLARE endMeterNum DECIMAL(8,3);
    DECLARE beginTime VARCHAR(20);
    DECLARE endTime VARCHAR(20);
    DECLARE orderNo VARCHAR(40);
    DECLARE orderStatus INT DEFAULT -1;
    DECLARE quantumDate VARCHAR(20);
    DECLARE tipPrice DECIMAL(8,4);
    DECLARE peakPrice DECIMAL(8,4);
    DECLARE usualPrice DECIMAL(8,4);
    DECLARE valleyPrice DECIMAL(8,4);
    DECLARE serviceCharge DECIMAL(10,4);

    SELECT transaction_number,ep_code,ep_gun_no,account_type,user_origin,user_account,trans_type
        ,to_char(to_date(begin_charge_time),'MM-dd HH:mm'),to_char(to_date(end_charge_time),'MM-dd HH:mm')
        ,tip_power/1000,peak_power/1000,usual_power/1000,valley_power/1000,total_power/1000,start_meter_num/1000,end_meter_num/1000
        ,case when type='0' charge_money/100 else charge_money/10000 end,case when type='0' service_money/100 else service_money/10000 end,start_soc,end_soc
    INTO transactionNumber,epCode,epGunNo,accountType,userOrigin,userAccount,transType
        ,beginTime,endTime
        ,tipPower,peakPower,usualPower,valleyPower,totalPower,startMeterNum,endMeterNum
        ,chargeAmt,serviceAmt,startSoc,endSoc
    FROM tbl_consume_record
    WHERE pk_consume_record = pkConsumeRecord;

    if (userOrigin = 1002) then
        set account = 'big1002';
        SELECT e.user_account, 3
        INTO account,userLevel
        FROM tbl_user e
        LEFT JOIN tbl_user_normal r
        ON r.user_id = e.user_id
        WHERE e.user_account = account;
    else
        SELECT r.user_id
        INTO userId
        FROM tbl_company e
        LEFT JOIN tbl_user_company r
        ON e.cpy_id = r.cpy_id
        WHERE e.cpy_number = userOrigin;
        if isnull(userId) then
            set rtn = 4;
            return;
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
                    12),select @@SCOPE_IDENTITY into pkCardId;
            else
                set rtn = 4;
                return;
            end if
        end if;
        SELECT e.user_account,case when e.user_leval=6 1 when e.user_leval=5 2 else 3 end
        INTO account,userLevel
        FROM tbl_user e
        LEFT JOIN tbl_user_normal r
        ON r.user_id = e.user_id
        WHERE e.user_id = userId;
    end if;
    if isnull(userLevel) then
        set rtn = 4;
        return;
    end if;

    call insert_charge_money(pkEpGunId, totalPower, chargeAmt, serviceAmt
    , beginTime, endTime, type, rtn);
    if (rtn = 0) then
        set totalAmt = chargeAmt + serviceAmt;
        set orderStatus = 3;
    else
        set totalAmt = 0;
        set orderStatus = 4;
    end if;
    set orderNo = f_make_orderno(pkEpGunId, userId);
    INSERT INTO tbl_ChargingOrder (
        chOr_Code,
        chOr_PileNumber,
        chOr_UserId,
        chOr_Type,
        chOr_Moeny,
        chOr_tipPower,
        chOr_peakPower,
        chOr_usualPower,
        chOr_valleyPower,
        chOr_QuantityElectricity,
        chOr_TimeQuantum,
        chOr_Muzzle,
        chOr_ChargingStatus,
        chOr_TransType,
        chOr_Createdate,
        chOr_Updatedate,
        chOr_UserName,
        chOr_TransactionNumber,
        chOr_OrderType,
        chOr_ChargeMoney,
        chOr_ServiceMoney,
        begin_charge_time,
        end_charge_time,
        pk_UserCard,
        chOr_UserOrigin,
        start_soc,
        end_soc,
        chOr_OrgNo,
        chor_parter_user_logo,
        chor_parter_extradata,
        chOr_CouponMoney, 
        pk_Coupon,
        chOr_Third_Type)
    VALUES (
        orderNo,
        epCode,
        userId,
        userLevel,
        totalAmt,
        tipPower,
        peakPower,
        usualPower,
        valleyPower,
        totalPower,
        concat(beginTime,' - ',endTime),
        epGunNo,
        orderStatus,
        transtype,
        SYSDATE(),
        SYSDATE(),
        '',
        transactionNumber,
        0,
        chargeAmt,
        serviceAmt,
        beginTime,
        endTime,
        pkCardId,
        userOrigin,
        startSoc,
        endSoc,
        userOrigin,
        userAccount,
        '',
        0 ,
        0,
        0);

    SELECT raIn_QuantumDate,raIn_TipTimeTariffPrice,raIn_PeakElectricityPrice,raIn_UsualPrice,raIn_ValleyTimePrice,raIn_ServiceCharge
    INTO quantumDate,tipPrice,peakPrice,usualPrice,valleyPrice,serviceCharge
    FROM tbl_RateInformation
    WHERE pk_RateInformation = rateId;
    insert into tbl_ChargingRecord (
        chRe_ElectricPileID,
        chRe_UsingMachineCode,
        chRe_TransactionNumber,
        chRe_ReservationNumber,
        chRe_StartDate,
        chRe_ChargingNumber,
        chRe_ChargingMethod,
        chRe_RestTime,
        chRe_Code,
        chRe_BeginShowsNumber,
        chRe_EndShowsNumber,
        user_phone,
        user_id,
        chRe_Status,
        chRe_JPrice,
        chRe_FPrice,
        chRe_PPrice,
        chRe_GPrice,
        chRe_ServiceCharge,
        chRe_QuantumDate,
        chRe_EndDate,
        pk_UserCard,
        chRe_OrgNo,
        chRe_PayMode,
        chre_parter_user_logo,
        chre_parter_extradata,
        chRe_ThirdCode,
        chRe_ThirdType
    ) values (
        pkEpGunId,
        epCode,
        transactionNumber,
        '',
        beginTime,
        epGunNo,
        if(userOrigin=1002,2,3),
        0,
        orderNo,
        to_char(startMeterNum),
        to_char(endMeterNum),
        account,
        userId,
        1,
        tipPrice,
        peakPrice,
        usualPrice,
        valleyPrice,
        serviceCharge,
        quantumDate,
        endTime,
        pkCardId,
        userOrigin,
        2,
        userAccount,
        '',
        0, 
        0
    );

    set rtn = 1;
END