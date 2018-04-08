DROP PROCEDURE charge_settlement_proc;
CREATE PROCEDURE charge_settlement_proc(pkConsumeRecord int, out rtn int)
top:BEGIN
    DECLARE transactionNumber VARCHAR(40);
    DECLARE epCode VARCHAR(40);
    DECLARE epGunNo INT;
    DECLARE pkElectricPile INT;
    DECLARE rateId INT;
    DECLARE pkEpGunId INT;
    DECLARE accountType INT;
    DECLARE userOrigin INT;
    DECLARE payMode INT DEFAULT 1;
    DECLARE orderStatus INT DEFAULT -1;
    DECLARE currentType INT;
    DECLARE type INT;

    SELECT transaction_number,ep_code,ep_gun_no,account_type,user_origin,calc_bit_type
    INTO transactionNumber,epCode,epGunNo,accountType,userOrigin,type
    FROM tbl_consume_record
    WHERE pk_consume_record = pkConsumeRecord;

    if (isnull(transactionNumber) or isnull(epCode)) then
        set rtn = -1;
        leave top;
    end if;
    if (LENGTH(transactionNumber) != 32 or transactionNumber = RPAD('0', 32, '0')) then
        set rtn = -2;
        leave top;
    end if;
    if (LENGTH(epCode) != 16 or epCode = RPAD('0', 16, '0')) then
        set rtn = -3;
        leave top;
    end if;

    SELECT chOr_ChargingStatus
    INTO orderStatus
    FROM tbl_chargingorder
    WHERE chOr_TransactionNumber = transactionNumber;
     if (orderStatus = 2 or orderStatus = 3 or orderStatus = 4) then
        set rtn = 0;
        leave top;
    end if;

    SELECT pk_ElectricPile,elPi_RateInformationId,elPi_ChargingMode
    INTO pkElectricPile,rateId,currentType
    FROM tbl_electricpile
    WHERE elPi_ElectricPileCode = epCode;
    if isnull(pkElectricPile) then
        set rtn = -4;
        leave top;
    end if;
    SELECT pk_ElectricpileHead
    INTO pkEpGunId
    FROM tbl_electricpilehead
    WHERE pk_ElectricPile = pkElectricPile
    AND ePHe_ElectricpileHeadId = epGunNo;
    if isnull(pkEpGunId) then
        set rtn = -5;
        leave top;
    end if;

    call insert_first_order(pkEpGunId, pkConsumeRecord, rateId,currentType, type, rtn);
    if (rtn < 1) then
        leave top;
    end if;

    set rtn = 1;
END