local voucherId = ARGV[1]
local userId = ARGV[2]
local stockKey = 'seckill:stock:'..voucherId
local orderKey = 'seckill:order:'..voucherId
--判断秒杀卷是否存在
if(redis.call('get',stockKey)==false)then
    return 3
end
--判断库存是否充足
if (tonumber(redis.call('get',stockKey))<=0) then
    return 1
end
--
if (tonumber(redis.call('sismember',orderKey,userId))==1) then
    return 2
end
--减库存
redis.call('incrby',stockKey,-1)
--
redis.call('sadd',orderKey,userId)
return 0