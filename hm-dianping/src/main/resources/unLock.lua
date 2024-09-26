if(redis.call('get',keys[1])==argv[1]) then
    return redis.call('del',keys[1])
end
return 0