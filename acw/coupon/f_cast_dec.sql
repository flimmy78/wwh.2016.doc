DROP FUNCTION f_cast_dec;
CREATE FUNCTION f_cast_dec(amt int, len int, roundlen int)
RETURNS decimal(10,4)
BEGIN
	declare ret decimal(10,4);

    if isnull(amt) then
        return 0;
    end if;
	set ret = round(amt/power(10, len), roundlen);

	RETURN (ret);
END;