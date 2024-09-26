package com.hmdp.controller;


import com.hmdp.dto.Result;
import com.hmdp.service.IFollowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/follow")
public class FollowController {
    @Autowired
    private IFollowService followService;

    @PutMapping("/{id}/{follow}")
    public Result follow(@PathVariable("id")Long followId,@PathVariable("follow")Boolean isFollow){
        return followService.follow(followId,isFollow);
    }
    @GetMapping("or/not/{id}")
    public Result followOrNot(@PathVariable("id")Long followId){
        return followService.followOrNot(followId);
    }
    @GetMapping("/common/{id}")
    public Result commonFollow(@PathVariable("id")Long targetId){
        return followService.commonFollow(targetId);
    }
}
