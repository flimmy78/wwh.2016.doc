DROP PROCEDURE p_vincode_stage;
CREATE PROCEDURE p_vincode_stage(transactionNumber VARCHAR(40), epCode VARCHAR(40), userId INT, vinCode INT, frozenAmt INT, payMode INT, type INT
    , totalPower INT, chargeAmt INT,serviceAmt INT, out totalAmt INT, out discountAmt INT,out pkCarVin INT)
BEGIN
    DECLARE pkCarVin INT;
    DECLARE servicePrice DECIMAL(8,4);
    DECLARE newServiceAmt DECIMAL(10,4);
    DECLARE totalAmt DECIMAL(10,4);
    DECLARE discountAmt DECIMAL(10,4);

    SELECT 
        pk_car_vin,
		cv_vin_code,
		cv_name,
		cv_servicemoney,
		cv_isdelete,
		cv_createdate,
		cv_createdate 
    INTO pkCarVin,servicePrice
    FROM 
		tbl_car_vin 
    WHERE 
        cv_vin_code=vinCode and cv_isdelete=0;
    if !isnull(servicePrice) then
        select if(type = 0, totalPower*servicePrice, totalPower*servicePrice*100) into newServiceAmt;
        set totalAmt = chargeAmt + newServiceAmt;
        if (payMode = 1) then
            if (chargeAmt+serviceAmt<=frozenAmt) then
                set discountAmt = serviceAmt - newServiceAmt;
            else
                set totalAmt = frozenAmt;
                set discountAmt = chargeAmt + serviceAmt - frozenAmt;
            end if;
        else
            set discountAmt = serviceAmt - newServiceAmt;
        end if;
    end if;

    if (discountAmt != 0) then
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
            6,
            current_date,
            if(type=1,f_cast_dec(discountAmt, 4, 4),f_cast_dec(discountAmt, 2, 2)),
            '',
            'VINÂëÓÅ»Ý',
            SYSDATE(),
            userId,
            0,
            epCode,
            transactionNumber
        );
    end if;
END