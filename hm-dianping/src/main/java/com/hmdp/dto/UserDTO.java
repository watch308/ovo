package com.hmdp.dto;

import com.hmdp.entity.User;
import lombok.Data;

@Data
public class UserDTO {
    private Long id;
    private String nickName;
    private String icon;
    public void toDTO(User user){
        this.id = user.getId();
        this.nickName = user.getNickName();
        this.icon= user.getIcon();
    }
}
