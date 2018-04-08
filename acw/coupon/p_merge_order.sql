DROP PROCEDURE p_merge_order;
CREATE PROCEDURE p_merge_order(pkEpGunId int, transactionNumber VARCHAR(40), epCode VARCHAR(40), epGunNo INT, userId INT, userLevel INT
    , tipPower DECIMAL(8,3), peakPower DECIMAL(8,3), usualPower DECIMAL(8,3), valleyPower DECIMAL(8,3), totalPower DECIMAL(8,3)
    , transType INT, beginTime INT, endTime INT, account VARCHAR(40), chargeAmt DECIMAL(10,4), serviceAmt DECIMAL(10,4), pkCardId INT, userOrigin INT
    , userAccount VARCHAR(40), token VARCHAR(40), pkCouPon INT, couPonAmt DECIMAL(10,4), thirdType INT,startSoc INT,endSoc INT
    , startMeterNum DECIMAL(8,3), endMeterNum DECIMAL(8,3), chargingMethod INT, payMode INT, serviceCharge DECIMAL(10,4), rateId INT
    , expFlag INT)
BEGIN
    DECLARE pkOrderId INT;
    DECLARE pkChargingRecord INT;
    DECLARE totalAmt DECIMAL(10,4);
    DECLARE orderStatus INT DEFAULT -1;
    DECLARE timeQuantum VARCHAR(40);

    DECLARE orderNo VARCHAR(40);
    DECLARE quantumDate VARCHAR(500);
    DECLARE tipPrice DECIMAL(8,4);
    DECLARE peakPrice DECIMAL(8,4);
    DECLARE usualPrice DECIMAL(8,4);
    DECLARE valleyPrice DECIMAL(8,4);

    if (expFlag = 0) then
        set totalAmt = chargeAmt + serviceAmt;
        if (payMode = 1) then
            set orderStatus = 2;
        else
            set orderStatus = 3;
        end if;
    else
        set totalPower = 0;
        set chargeAmt = 0;
        set serviceAmt = 0;
        set totalAmt = 0;
        set orderStatus = 4;
    end if;

    SELECT pk_ChargingOrder
    INTO pkOrderId
    FROM tbl_chargingorder
    WHERE chOr_TransactionNumber = transactionNumber;
    if (isnull(pkOrderId)) then
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
            concat(FROM_UNIXTIME(beginTime,'%m-%d %h:%i'),' - ',FROM_UNIXTIME(endTime,'%m-%d %h:%i')),
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
            FROM_UNIXTIME(beginTime,'%Y-%m-%d %h:%i:%s'),
            FROM_UNIXTIME(endTime,'%Y-%m-%d %h:%i:%s'),
            pkCardId,
            userOrigin,
            startSoc,
            endSoc,
            userOrigin,
            userAccount,
            token,
            couPonAmt,
            pkCouPon,
            thirdType);
    else
        UPDATE tbl_ChargingOrder SET
            chOr_Moeny=totalAmt,
            chOr_QuantityElectricity=totalPower,
            chOr_TimeQuantum=concat(FROM_UNIXTIME(beginTime,'%m-%d %h:%i'),' - ',FROM_UNIXTIME(endTime,'%m-%d %h:%i')),
            chOr_TransType=transtype,
            chOr_UserName='',
            chOr_OrderType=0,
            chOr_tipPower=tipPower,
            chOr_peakPower=peakPower,
            chOr_usualPower=usualPower,
            chOr_valleyPower=valleyPower,
            chOr_ChargingStatus= orderStatus,
            chOr_TransType= transtype,
            chOr_Updatedate=SYSDATE(),
            chOr_ChargeMoney=chargeAmt,
            chOr_ServiceMoney =serviceAmt,
            begin_charge_time = FROM_UNIXTIME(beginTime,'%Y-%m-%d %h:%i:%s'),
            end_charge_time = FROM_UNIXTIME(endTime,'%Y-%m-%d %h:%i:%s'),
            start_soc = startSoc,
            end_soc = endSoc,
            chOr_CouponMoney = couPonAmt ,
            pk_Coupon = pkCoupon ,
            chOr_Third_Type = thirdType
        WHERE 
            chOr_TransactionNumber=transactionNumber; 
    end if;

    SELECT raIn_QuantumDate,raIn_TipTimeTariff,raIn_PeakElectricityPrice,raIn_UsualPrice,raIn_ValleyTimePrice,if(isnull(serviceCharge), raIn_ServiceCharge, serviceCharge)
    INTO quantumDate,tipPrice,peakPrice,usualPrice,valleyPrice,serviceCharge
    FROM tbl_RateInformation
    WHERE pk_RateInformation = rateId;

    SELECT pk_ChargingRecord
    INTO pkChargingRecord
    FROM tbl_ChargingRecord
    WHERE chRe_TransactionNumber = transactionNumber;
    if isnull(pkChargingRecord) then
        insert into tbl_ChargingRecord (
            chRe_ElectricPileID,
            chRe_UsingMachineCode,
            chRe_TransactionNumber,
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
            FROM_UNIXTIME(beginTime),
            epGunNo,
            chargingMethod,
            0,
            orderNo,
            cast(startMeterNum as char),
            cast(endMeterNum as char),
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
            payMode,
            userAccount,
            token,
            pkCouPon, 
            thirdType
        );
    else
        UPDATE tbl_ChargingRecord SET
            chRe_EndDate=FROM_UNIXTIME(endTime),
            chRe_EndShowsNumber=cast(endMeterNum as char),
            chRe_Status = 1,
            chRe_ServiceCharge = serviceCharge,
            chRe_ThirdCode =pkCoupon,
            chRe_ThirdType =thirdType
        WHERE
            chRe_TransactionNumber=transactionNumber;
    end if;
END