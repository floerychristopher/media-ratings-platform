package mrp.service;

import mrp.auth.TokenManager;
import mrp.model.User;
import mrp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mindrot.jbcrypt.BCrypt;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    // @Mock erstellt "Fake" Version des Repositories
    @Mock
    private UserRepository userRepository;

    @Mock
    private TokenManager tokenManager;

    // erstellt echten UserService und injiziert Fake Abhängigkeiten
    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Vor jedem Test einen frischen Test-User anlegen
        testUser = new User("testuser", BCrypt.hashpw("secret123", BCrypt.gensalt()));
        testUser.setId(1);
    }

    // --- REGISTRIERUNG TESTS ---

    @Test
    void register_ValidData_CreatesUserSuccessfully() throws Exception {
        // Arrange: Wenn nach Username gesucht wird, gib null zurück (User existiert noch nicht)
        when(userRepository.findByUsername("newuser")).thenReturn(null);
        // Wenn create aufgerufen wird, gib den User zurück
        when(userRepository.create(any(User.class))).thenReturn(new User("newuser", "hashed"));

        // Act (Ausführen)
        User result = userService.register("newuser", "pass123");

        // Assert (Überprüfen)
        assertNotNull(result);
        assertEquals("newuser", result.getUsername());
        verify(userRepository, times(1)).create(any(User.class)); // Wurde create 1x aufgerufen?
    }

    @Test
    void register_EmptyUsername_ThrowsIllegalArgumentException() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.register("", "pass123");
        });
        assertEquals("Username is required", exception.getMessage());
    }

    @Test
    void register_UserAlreadyExists_ThrowsIllegalStateException() throws Exception {
        // Arrange: Fake, dass User schon in DB ist
        when(userRepository.findByUsername("existinguser")).thenReturn(testUser);

        // Act & Assert
        Exception exception = assertThrows(IllegalStateException.class, () -> {
            userService.register("existinguser", "pass123");
        });
        assertEquals("Username already exists", exception.getMessage());
    }

    // --- LOGIN TESTS ---

    @Test
    void login_ValidCredentials_ReturnsToken() throws Exception {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(testUser);
        when(tokenManager.generateToken(testUser)).thenReturn("testuser-mrpToken");

        // Act
        String token = userService.login("testuser", "secret123");

        // Assert
        assertEquals("testuser-mrpToken", token);
    }

    @Test
    void login_WrongPassword_ThrowsIllegalArgumentException() throws Exception {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(testUser);

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            // Falsches Passwort übergeben
            userService.login("testuser", "wrongpassword");
        });
        assertEquals("Invalid credentials", exception.getMessage());
    }

    @Test
    void login_UserNotFound_ThrowsIllegalArgumentException() throws Exception {
        // Arrange
        when(userRepository.findByUsername("unknown")).thenReturn(null);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            userService.login("unknown", "secret123");
        });
    }

    // --- PROFILE TESTS ---

    @Test
    void updateProfile_OwnProfile_UpdatesSuccessfully() throws Exception {
        // Arrange
        User authUser = new User();
        authUser.setUsername("testuser");

        when(userRepository.findByUsername("testuser")).thenReturn(testUser);

        // Act
        User result = userService.updateProfile(authUser, "testuser", "New Bio");

        // Assert
        assertEquals("New Bio", result.getBio());
        verify(userRepository, times(1)).update(testUser); // Prüfen ob Update aufgerufen wurde
    }

    @Test
    void updateProfile_OtherUsersProfile_ThrowsSecurityException() {
        // Arrange
        User authUser = new User();
        authUser.setUsername("hacker"); // Versucht das Profil von 'testuser' zu ändern

        // Act & Assert
        Exception exception = assertThrows(SecurityException.class, () -> {
            userService.updateProfile(authUser, "testuser", "Hacked Bio!");
        });
        assertEquals("Cannot edit another user's profile", exception.getMessage());
    }
}