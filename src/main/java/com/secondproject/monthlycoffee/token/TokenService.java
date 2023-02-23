package com.secondproject.monthlycoffee.token;

import com.secondproject.monthlycoffee.config.security.JwtProperties;
import com.secondproject.monthlycoffee.config.security.JwtUtil;
import com.secondproject.monthlycoffee.dto.member.MemberDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

@RequiredArgsConstructor
@Transactional
@Service
public class TokenService {
    private final RefreshTokenRepository refreshTokenRepo;
    private final AccessTokenBlackListRepository accessTokenBlackListRepo;
    private final JwtUtil jwtUtil;

    public TokenDto login(MemberDto memberDto) {
        String access = jwtUtil.createAccess(memberDto.id(), JwtProperties.ACCESS_EXPIRATION_TIME);
        String refresh =  jwtUtil.createRefresh(JwtProperties.REFRESH_EXPIRATION_TIME);
        refreshTokenRepo.save(new RefreshToken(refresh, memberDto.id(), JwtProperties.REFRESH_EXPIRATION_TIME));
        return new TokenDto(access, refresh);
    }

    public void logout(final long memberId, final String access) {
        String resolvedAccessToken = jwtUtil.resolve(access);
        long accessExpiration = jwtUtil.getAccessExpiration(resolvedAccessToken);
        accessTokenBlackListRepo.save(new AccessTokenBlackList(resolvedAccessToken, accessExpiration));
        refreshTokenRepo.findAll().stream()
                .filter(r -> Objects.nonNull(r) && r.getMemberId() == memberId)
                .forEach(refreshTokenRepo::delete);
    }

    public String reissue(final String refresh) {
        jwtUtil.verifyRefresh(refresh);
        RefreshToken refreshToken = refreshTokenRepo.findById(refresh).orElseThrow(() -> new NoSuchElementException("등록되지않은 리프레쉬 토큰입니다."));
        return jwtUtil.createAccess(refreshToken.getMemberId(), JwtProperties.ACCESS_EXPIRATION_TIME);
    }


}
