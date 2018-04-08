DROP FUNCTION f_make_orderno;
CREATE FUNCTION f_make_orderno(pkEpGunId int, chargeUserId int)
RETURNS VARCHAR(40)
BEGIN
	declare orderno VARCHAR(40);

    if (isnull(pkEpGunId) or isnull(chargeUserId)) then
        return '';
    end if;
	set orderno = CONCAT(UNIX_TIMESTAMP(SYSDATE()),pkEpGunId mod 100000,chargeUserId mod 1000000);

	RETURN (orderno);
END;