package com.loopers.application.user;

import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
public class UserFacade {

    private final UserService userService;

    @Transactional(readOnly = true)
    public UserInfo getMyInfo(String loginId, String rawPassword) {
        User user = userService.authenticate(loginId, rawPassword);
        return UserInfo.fromWithMaskedName(user);
    }
}
