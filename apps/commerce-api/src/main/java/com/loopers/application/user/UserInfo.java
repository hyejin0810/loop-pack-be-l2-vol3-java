package com.loopers.application.user;

import com.loopers.domain.user.User;

public record UserInfo(String loginId, String name, String birthday, String email, Long balance) {

    public static UserInfo from(User user) {
        return new UserInfo(
            user.getLoginId(),
            user.getName(),
            user.getBirthday(),
            user.getEmail(),
            user.getBalance()
        );
    }

    public static UserInfo fromWithMaskedName(User user) {
        return new UserInfo(
            user.getLoginId(),
            user.getMaskedName(),
            user.getBirthday(),
            user.getEmail(),
            user.getBalance()
        );
    }
}
