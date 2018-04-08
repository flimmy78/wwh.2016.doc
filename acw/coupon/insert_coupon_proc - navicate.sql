DROP PROCEDURE insert_coupon_proc;
CREATE PROCEDURE insert_coupon_proc(cpUserid int, pkActivity int)
BEGIN
    DECLARE pkCouponvariety INT;
    DECLARE covaLimitation INT;
    DECLARE covaCouponValue INT;
    DECLARE covaCouponCondition INT;
    DECLARE covaCouponTerm INT;
    DECLARE num INT DEFAULT 0;
    DECLARE acscNum INT;
    DECLARE no_more_record INT DEFAULT 0;
    DECLARE cur_record CURSOR FOR 
        SELECT pk_CouponVariety,acsc_num 
        FROM tbl_activityschedule
        WHERE pk_Activity = pkActivity;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET no_more_record = 1;
    
    OPEN cur_record;
    FETCH cur_record INTO pkCouponvariety, acscNum;
    WHILE no_more_record != 1 DO
        SET num = 0;
        SELECT cova_Limitation,cova_CouponValue,cova_CouponCondition,cova_CouponTerm
        INTO covaLimitation,covaCouponValue,covaCouponCondition,covaCouponTerm
        FROM tbl_couponvariety
        WHERE pk_CouponVariety = pkCouponvariety;

        WHILE num<acscNum DO
            INSERT INTO tbl_coupon(
                pk_activity,
                pk_couponvariety,
                cp_limitation,
                cp_couponvalue,
                cp_couponcondition,
                cp_userid,
                cp_begindate,
                cp_enddate)
            VALUES(
                pkActivity,
                pkCouponvariety,
                covaLimitation,
                covaCouponValue,
                covaCouponCondition,
                cpUserid,
                current_date,
                current_date + covaCouponTerm);
            set num = num + 1;
        END WHILE;
        FETCH cur_record INTO pkCouponvariety, acscNum;
    END WHILE;
    CLOSE cur_record;

END