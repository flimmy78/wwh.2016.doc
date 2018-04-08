DROP PROCEDURE p_coupon_stage;
CREATE PROCEDURE p_coupon_stage(transactionNumber VARCHAR(40), epCode VARCHAR(40), userId INT, totalAmt DECIMAL(10,4), currentType INT
    , out couponAmt DECIMAL(10,4), out realCouponAmt DECIMAL(10,4),out pkCoupon INT)
top:BEGIN
    DECLARE acStatus INT;
    DECLARE dcStatus INT;
    DECLARE userFirst INT DEFAULT 0;
    DECLARE cpStatus INT;
    DECLARE cpLimit INT;
    DECLARE cpRate VARCHAR(10);

    SELECT max(ac_status),max(dc_status)
    INTO acStatus,dcStatus
    FROM tbl_user_newcoupon 
    WHERE
        user_id=userId;
    if isnull(acStatus) then
        SELECT if(count(elPi_ChargingMode)>0,1,0)
        INTO acStatus
        FROM 
            tbl_chargingorder a,tbl_electricpile b
        WHERE 
            a.chOr_PileNumber = b.elPi_ElectricPileCode and b.elPi_ChargingMode=14
        and a.chOr_ChargingStatus>=2 and a.chOr_UserId = userId ;
        SELECT if(count(elPi_ChargingMode)>0,1,0)
        INTO dcStatus
        FROM 
            tbl_chargingorder a,tbl_electricpile b
        WHERE 
            a.chOr_PileNumber = b.elPi_ElectricPileCode and b.elPi_ChargingMode=5
        and a.chOr_ChargingStatus>=2 and a.chOr_UserId = userId ;

        INSERT INTO tbl_user_newcoupon (
            user_id,
            ac_status,	
            dc_status
        ) VALUES (
            userId,
            acStatus,
            dcStatus
        );
    end if;
    if (acStatus = 0 and currentType = 14) or (dcStatus = 0 and currentType = 5) then
        set userFirst = 1;
    end if;
    if (currentType = 14) then
        set cpStatus = acStatus;
        set cpLimit = 1;
    else
        set cpStatus = dcStatus;
        set cpLimit = 2;
    end if;
    if (cpStatus = 0) then
        SELECT pk_coupon,cp_couponvalue,if(cp_couponcondition=0, cp_couponvalue,
        	if(totalAmt>=cp_couponcondition,cp_couponvalue/cp_couponcondition,0)) cp_rate
        INTO pkCouPon,couponAmt,cpRate
        FROM
		tbl_coupon
        WHERE cp_status = 0
            AND (cp_limitation = cpLimit OR cp_limitation = 3)
		    AND cp_userid = userId
		    AND (current_date BETWEEN cp_begindate AND cp_enddate)
		    	AND pk_activity IN 
		    	(SELECT pk_activity
		    	FROM tbl_activity
		    	WHERE act_Status = 0
		    	AND (current_date BETWEEN act_BeginDate AND act_EndDate)
                AND act_ActivityRule = 2)
            ORDER BY if (cp_rate = 0,'9999-99-99', cp_enddate) ASC,cp_rate DESC limit 1;
    end if;
    if (cpStatus != 0 or isnull(cpRate)) then
        SELECT pk_coupon,cp_couponvalue
	        ,if(cp_couponcondition=0,if(totalAmt>=cp_couponvalue,cp_couponvalue,0),
        	if(totalAmt>=cp_couponcondition,cp_couponvalue/cp_couponcondition,0)) cp_rate
        INTO pkCouPon,couponAmt,cpRate
        FROM
		tbl_coupon
        WHERE cp_status = 0
            AND (cp_limitation = cpLimit OR cp_limitation = 3)
		    AND cp_userid = userId
		    AND (current_date BETWEEN cp_begindate AND cp_enddate)
		    	AND pk_activity NOT IN 
		    	(SELECT pk_activity
		    	FROM tbl_activity
		    	WHERE act_Status = 0
		    	AND (current_date BETWEEN act_BeginDate AND act_EndDate)
                AND act_ActivityRule = 2)
            ORDER BY if (cp_rate = 0,'9999-99-99', cp_enddate) ASC,cp_rate DESC limit 1;
    end if;
    if (isnull(cpRate)) then
        set pkCoupon = 0;
        set couponAmt = 0;
        set realCouponAmt = 0;
        leave top;
    end if;

    UPDATE tbl_coupon
    set
    cp_status = 1,
    cp_couponcode=if(TRIM(cp_couponcode)!='',CONCAT(TRIM(cp_couponcode),'_1'),cp_couponcode),
    cp_updatedate = now()
    WHERE
    pk_coupon =pkCoupon;

    select if(totalAmt >= couPonAmt, couPonAmt, totalAmt) into realCouponAmt;
    if (realCouponAmt != 0) then
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
            realCouponAmt,
            '',
            '”≈ª›»Øµ÷ø€',
            SYSDATE(),
            userId,
            0,
            epCode,
            transactionNumber
        );
    end if;
END