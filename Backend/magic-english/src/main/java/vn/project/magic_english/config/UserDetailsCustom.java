package vn.project.magic_english.config;

import java.util.Collections;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import vn.project.magic_english.service.UserService;

@Component("userDetailsService")
public class UserDetailsCustom implements UserDetailsService {

    private final UserService userService;

    public UserDetailsCustom(UserService userService) {
        this.userService = userService;
    }

    /**
     * Hàm loadUserByUsername được Spring Security gọi khi xác thực đăng nhập.
     * - Nhận vào username (thường là email hoặc tên đăng nhập).
     * - Gọi userService để lấy thông tin user từ database.
     * - Nếu không tìm thấy user, ném ra exception để báo lỗi đăng nhập.
     * - Nếu tìm thấy, trả về đối tượng UserDetails (ở đây là User của Spring
     * Security),
     * gồm: username (email), password (đã mã hóa), và danh sách quyền (ở đây
     * hardcode ROLE_USER).
     * => Kết quả này sẽ được Spring Security dùng để xác thực và cấp quyền cho
     * user.
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        vn.project.magic_english.model.User user = this.userService.handleGetUserByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("Username/password không hợp lệ");
        }

        // Trả về UserDetails cho Spring Security sử dụng xác thực
        return new User(
                user.getEmail(), // username
                user.getPassword(), // password đã mã hóa
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")) // quyền (role)
        );
    }

}
