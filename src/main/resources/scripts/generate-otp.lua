-- KEYS[1] = otp key
-- KEYS[2] = cooldown key
-- ARGV[1] = otp hmac to store
-- ARGV[2] = otp ttl in milliseconds
-- ARGV[3] = cooldown in milliseconds
--
-- Returns:  0   = saved, and the cooldown started
--           > 0 = cooldown still running: milliseconds left (nothing was changed)

-- 1) Cooldown running? Refuse, and leave the current OTP untouched.
local remaining = redis.call('PTTL', KEYS[2])
if remaining > 0 then
    return remaining
end

-- 2) Store the OTP and start the cooldown. Both happen, or neither (the script is atomic).
redis.call('SET', KEYS[1], ARGV[1], 'PX', ARGV[2])
redis.call('SET', KEYS[2], '1', 'PX', ARGV[3])
return 0