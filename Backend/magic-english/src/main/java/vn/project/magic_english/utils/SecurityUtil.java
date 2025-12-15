package vn.project.magic_english.utils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;

import com.nimbusds.jose.util.Base64;

import vn.project.magic_english.model.response.ResLoginDTO;

@Service
// SecurityUtil: Tiện ích bảo mật cho JWT, xác thực, lấy thông tin user hiện tại
public class SecurityUtil {

    private final JwtEncoder jwtEncoder;

    public SecurityUtil(JwtEncoder jwtEncoder) {
        this.jwtEncoder = jwtEncoder;
    }

    // Thuật toán mã hóa JWT sử dụng HMAC SHA-512
    public static final MacAlgorithm JWT_ALGORITHM = MacAlgorithm.HS256;

    // Secret key (base64) dùng để ký JWT, lấy từ cấu hình
    @Value("${magic_english.jwt.base64-secret}")
    private String jwtKey;

    // Thời gian sống của access token (giây), lấy từ cấu hình
    @Value("${magic_english.jwt.access-token-validity-in-seconds}")
    private long accessTokenExpiration;

    // Thời gian sống của refresh token (giây), lấy từ cấu hình
    @Value("${magic_english.jwt.refresh-token-validity-in-seconds}")
    private long refreshTokenExpiration;

    /**
     * Tạo access token (JWT) cho user đăng nhập thành công.
     * 
     * @param email email user (subject của token)
     * @param dto   thông tin user trả về khi login
     * @return access token dạng JWT string
     */
    public String createAccessToken(String email, ResLoginDTO dto) {
        // Tạo đối tượng user nhúng vào token
        ResLoginDTO.UserInsideToken userToken = new ResLoginDTO.UserInsideToken();
        userToken.setId(dto.getUser().getId());
        userToken.setEmail(dto.getUser().getEmail());
        userToken.setName(dto.getUser().getName());

        // Thời điểm hiện tại và thời điểm hết hạn token
        Instant now = Instant.now();
        Instant validity = now.plus(this.accessTokenExpiration, ChronoUnit.SECONDS);

        // Danh sách quyền hardcode (chỉ để test, thực tế nên lấy từ DB)
        List<String> listAuthority = new ArrayList<String>();
        listAuthority.add("ROLE_USER_CREATE");
        listAuthority.add("ROLE_USER_UPDATE");

        // Xây dựng claims cho JWT
        // Xây dựng các claims cho JWT:
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuedAt(now) // Thời điểm phát hành token (thường là hiện tại)
                .expiresAt(validity) // Thời điểm token hết hạn (now + accessTokenExpiration)
                .subject(email) // Định danh chính của token, ở đây là email user (subject)
                .claim("user", userToken) // Thông tin user (id, email, name) sẽ được nhúng vào payload token
                .claim("permission", listAuthority) // Danh sách quyền (roles/permissions) của user
                .build();

        // Tạo header cho JWT với thuật toán HS512
        JwsHeader jwsHeader = JwsHeader.with(JWT_ALGORITHM).build();
        // Mã hóa claims và header thành JWT string
        return this.jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();
    }

    /**
     * Tạo refresh token (JWT) cho user.
     * 
     * @param email email user (subject của token)
     * @param dto   thông tin user trả về khi login
     * @return refresh token dạng JWT string
     */
    public String createRefreshToken(String email, ResLoginDTO dto) {
        // Thời điểm hiện tại và thời điểm hết hạn token
        Instant now = Instant.now();
        Instant validity = now.plus(this.refreshTokenExpiration, ChronoUnit.SECONDS);

        // Tạo đối tượng user nhúng vào token
        ResLoginDTO.UserInsideToken userToken = new ResLoginDTO.UserInsideToken();
        userToken.setId(dto.getUser().getId());
        userToken.setEmail(dto.getUser().getEmail());
        userToken.setName(dto.getUser().getName());

        // Xây dựng claims cho JWT (refresh token không có permission)
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuedAt(now)
                .expiresAt(validity)
                .subject(email)
                .claim("user", userToken)
                .build();

        // Tạo header cho JWT với thuật toán HS512
        JwsHeader jwsHeader = JwsHeader.with(JWT_ALGORITHM).build();
        // Mã hóa claims và header thành JWT string
        return this.jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();
    }

    /**
     * Lấy secret key từ chuỗi base64 trong cấu hình để ký/giải mã JWT
     */
    private SecretKey getSecretKey() {
        byte[] keyBytes = Base64.from(jwtKey).decode();
        return new SecretKeySpec(keyBytes, 0, keyBytes.length,
                JWT_ALGORITHM.getName());
    }

    /**
     * Kiểm tra tính hợp lệ của refresh token (giải mã, xác thực chữ ký, kiểm tra
     * hạn)
     * 
     * @param token refresh token cần kiểm tra
     * @return đối tượng Jwt nếu hợp lệ, throw exception nếu không hợp lệ
     */
    public Jwt checkValidRefreshToken(String token) {
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withSecretKey(
                getSecretKey()).macAlgorithm(SecurityUtil.JWT_ALGORITHM).build();
        try {
            return jwtDecoder.decode(token);
        } catch (Exception e) {
            System.out.println(">>> Refresh Token error: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Lấy username/email của user hiện tại từ SecurityContext
     * 
     * @return username/email nếu có, Optional rỗng nếu chưa đăng nhập
     */
    public static Optional<String> getCurrentUserLogin() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        return Optional.ofNullable(extractPrincipal(securityContext.getAuthentication()));
    }

    /**
     * Trích xuất thông tin định danh (username/email) từ Authentication
     */
    private static String extractPrincipal(Authentication authentication) {
        if (authentication == null) {
            return null;
        } else if (authentication.getPrincipal() instanceof UserDetails springSecurityUser) {
            return springSecurityUser.getUsername();
        } else if (authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt.getSubject();
        } else if (authentication.getPrincipal() instanceof String s) {
            return s;
        }
        return null;
    }

    /**
     * Lấy JWT token của user hiện tại từ SecurityContext
     * 
     * @return JWT string nếu có, Optional rỗng nếu chưa đăng nhập
     */
    public static Optional<String> getCurrentUserJWT() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        return Optional.ofNullable(securityContext.getAuthentication())
                .filter(authentication -> authentication.getCredentials() instanceof String)
                .map(authentication -> (String) authentication.getCredentials());
    }

    /**
     * Check if a user is authenticated.
     *
     * @return true if the user is authenticated, false otherwise.
     */
    // public static boolean isAuthenticated() {
    // Authentication authentication =
    // SecurityContextHolder.getContext().getAuthentication();
    // return authentication != null &&
    // getAuthorities(authentication).noneMatch(AuthoritiesConstants.ANONYMOUS::equals);
    // }

    /**
     * Checks if the current user has any of the authorities.
     *
     * @param authorities the authorities to check.
     * @return true if the current user has any of the authorities, false otherwise.
     */
    // public static boolean hasCurrentUserAnyOfAuthorities(String... authorities) {
    // Authentication authentication =
    // SecurityContextHolder.getContext().getAuthentication();
    // return (
    // authentication != null && getAuthorities(authentication).anyMatch(authority
    // -> Arrays.asList(authorities).contains(authority))
    // );
    // }

    /**
     * Checks if the current user has none of the authorities.
     *
     * @param authorities the authorities to check.
     * @return true if the current user has none of the authorities, false
     *         otherwise.
     */
    // public static boolean hasCurrentUserNoneOfAuthorities(String... authorities)
    // {
    // return !hasCurrentUserAnyOfAuthorities(authorities);
    // }

    /**
     * Checks if the current user has a specific authority.
     *
     * @param authority the authority to check.
     * @return true if the current user has the authority, false otherwise.
     */
    // public static boolean hasCurrentUserThisAuthority(String authority) {
    // return hasCurrentUserAnyOfAuthorities(authority);
    // }

    // private static Stream<String> getAuthorities(Authentication authentication) {
    // return
    // authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority);
    // }

}
