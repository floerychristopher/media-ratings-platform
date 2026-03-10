package mrp.service;

import mrp.model.Media;
import mrp.model.Rating;
import mrp.repository.MediaRepository;
import mrp.repository.RatingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RatingServiceTest {

    @Mock
    private RatingRepository ratingRepo;

    @Mock
    private MediaRepository mediaRepo;

    @InjectMocks
    private RatingService ratingService;

    // --- RATE MEDIA TESTS ---

    @Test
    void rateMedia_ValidData_CreatesRatingSuccessfully() throws Exception {
        // Arrange
        Media media = new Media();
        media.setId(1);
        when(mediaRepo.getById(1)).thenReturn(media);

        Rating newRating = new Rating();
        newRating.setId(100);
        when(ratingRepo.create(any(Rating.class))).thenReturn(newRating);

        // Act
        Rating result = ratingService.rateMedia(1, 2, 4, "Good!");

        // Assert
        assertNotNull(result);
        assertEquals(100, result.getId());
        verify(ratingRepo, times(1)).create(any(Rating.class));
    }

    @Test
    void rateMedia_StarsTooHigh_ThrowsIllegalArgumentException() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            ratingService.rateMedia(1, 2, 6, "Too many stars!");
        });
        assertEquals("Stars must be between 1 and 5", exception.getMessage());
    }

    @Test
    void rateMedia_StarsTooLow_ThrowsIllegalArgumentException() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            ratingService.rateMedia(1, 2, 0, "Too few stars!");
        });
        assertEquals("Stars must be between 1 and 5", exception.getMessage());
    }

    @Test
    void rateMedia_MediaNotFound_ThrowsIllegalArgumentException() throws Exception {
        // Arrange
        when(mediaRepo.getById(99)).thenReturn(null);

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            ratingService.rateMedia(99, 2, 5, "Awesome");
        });
        assertEquals("Media not found", exception.getMessage());
    }

    @Test
    void rateMedia_UserAlreadyRated_ThrowsIllegalStateException() throws Exception {
        // Arrange
        Media media = new Media();
        media.setId(1);
        when(mediaRepo.getById(1)).thenReturn(media);

        // Simuliere den DB-Fehler (Unique Constraint Violation)
        when(ratingRepo.create(any(Rating.class))).thenThrow(new SQLException("Unique violation"));

        // Act & Assert
        Exception exception = assertThrows(IllegalStateException.class, () -> {
            ratingService.rateMedia(1, 2, 5, "Awesome");
        });
        assertEquals("User has already rated this media", exception.getMessage());
    }

    // --- MODERATION (CONFIRM) TESTS ---

    @Test
    void confirmRating_AsCreator_Success() throws Exception {
        // Arrange
        Rating rating = new Rating();
        rating.setMediaId(1);
        when(ratingRepo.getById(100)).thenReturn(rating);

        Media media = new Media();
        media.setId(1);
        media.setCreatedBy(5); // User 5 ist der Ersteller
        when(mediaRepo.getById(1)).thenReturn(media);

        when(ratingRepo.confirmComment(100)).thenReturn(true);

        // Act
        boolean result = ratingService.confirmRating(100, 5); // User 5 fragt an

        // Assert
        assertTrue(result);
        verify(ratingRepo, times(1)).confirmComment(100);
    }

    @Test
    void confirmRating_NotCreator_ThrowsSecurityException() throws Exception {
        // Arrange
        Rating rating = new Rating();
        rating.setMediaId(1);
        when(ratingRepo.getById(100)).thenReturn(rating);

        Media media = new Media();
        media.setId(1);
        media.setCreatedBy(5); // User 5 ist der Ersteller
        when(mediaRepo.getById(1)).thenReturn(media);

        // Act & Assert
        Exception exception = assertThrows(SecurityException.class, () -> {
            ratingService.confirmRating(100, 2); // User 2 (NICHT der Ersteller) versucht es
        });
        assertEquals("Only the creator of the media can confirm comments", exception.getMessage());
    }

    @Test
    void confirmRating_RatingNotFound_ThrowsIllegalArgumentException() throws Exception {
        // Arrange
        when(ratingRepo.getById(99)).thenReturn(null);

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            ratingService.confirmRating(99, 5);
        });
        assertEquals("Rating not found", exception.getMessage());
    }
}