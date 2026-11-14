-- KEYS[1] = otp key            (holds the OTP HMAC, short TTL)
-- KEYS[2] = fail-counter key   (per purpose + subject, NOT per OTP)
-- ARGV[1] = otp hmac to check
-- ARGV[2] = max failed attempts allowed in the window
-- ARGV[3] = fail window in milliseconds
--
-- Returns:  1 = success (OTP consumed, counter cleared)
--           0 = invalid or expired (failure counted)
--          -1 = locked (too many failures, OTP not even checked)

-- 1) Locked? Refuse before looking at the OTP at all.
local failures = tonumber(redis.call('GET', KEYS[2]) or '0')
if failures >= tonumber(ARGV[2]) then
    return -1
end

-- 2) Correct OTP: consume it and clear the failure counter.
local stored = redis.call('GET', KEYS[1])
if stored and stored == ARGV[1] then
    redis.call('DEL', KEYS[1], KEYS[2])
    return 1
end

-- 3) Wrong (or missing/expired) OTP: count the failure.
--    Counted even when no OTP exists, so the response never reveals whether the account has one.
failures = redis.call('INCR', KEYS[2])
if failures == 1 then
    redis.call('PEXPIRE', KEYS[2], ARGV[3])   -- the window starts at the first failure
end

-- 4) Limit reached: kill the current OTP so a fresh one must be requested after the lock expires.
if failures >= tonumber(ARGV[2]) then
    redis.call('DEL', KEYS[1])
end

return 0