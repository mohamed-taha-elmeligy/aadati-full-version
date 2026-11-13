local value = redis.call('GET', KEYS[1])

if value == ARGV[1] then
    return redis.call('DEL', KEYS[1])
end

return 0