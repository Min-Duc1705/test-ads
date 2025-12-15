package vn.project.magic_english.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import lombok.NoArgsConstructor;
import vn.project.magic_english.model.User;
import vn.project.magic_english.model.request.ReqUpdateUser;
import vn.project.magic_english.repository.UserRepository;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    // Cập nhật avatar cho user
    public User updateAvatar(long userId, String avatarUrl) {
        User user = handleGetUserById(userId);
        if (user != null) {
            user.setAvatarUrl(avatarUrl);
            return userRepository.save(user);
        }
        return null;
    }

    public User handleCreatUser(User newUser) {
        return this.userRepository.save(newUser);
    }

    public User handleGetUserById(long id) {
        Optional<User> user = this.userRepository.findById(id);
        if (user.isPresent()) {
            return user.get();
        }
        return null;
    }

    public void handleDeleteUser(long id) {
        this.userRepository.deleteById(id);
    }

    public User handleUpdateUser(ReqUpdateUser updatedUser) {
        User existingUser = this.handleGetUserById(updatedUser.getId());
        if (existingUser != null) {
            existingUser.setName(updatedUser.getName());
            existingUser.setEmail(updatedUser.getEmail());
            existingUser.setAvatarUrl(updatedUser.getAvatarUrl());
            return this.userRepository.save(existingUser);
        }
        return null;
    }

    public User handleGetUserByUsername(String username) {
        return this.userRepository.findByEmail(username);
    }

    public boolean isEmailExist(String email) {
        return this.userRepository.existsByEmail(email);
    }

    public void updateUserToken(String token, String email) {
        User currentUser = this.handleGetUserByUsername(email);
        if (currentUser != null) {
            currentUser.setRefreshToken(token);
            this.userRepository.save(currentUser);
        }
    }

    public User getUserByRefreshTokenAndEmail(String token, String email) {
        return this.userRepository.findByRefreshTokenAndEmail(token, email);
    }
}
