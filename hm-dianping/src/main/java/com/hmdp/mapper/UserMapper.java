package com.hmdp.mapper;
import java.util.List;

import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface UserMapper extends BaseMapper<User> {
    User selectUserByPhone(String phone);

    User selectOneById(@Param("id") Long id);

    List<UserDTO> selectAllById(List<Long>id);

    int insertSelective(User user);

    String selectPasswordByPhone(@Param("phone") String phone);

    int updateSelective(User user);
    List<UserDTO> searchAllById(List<Long> id);
}

