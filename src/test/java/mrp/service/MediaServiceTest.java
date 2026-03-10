package mrp.service;

import mrp.model.Media;
import mrp.repository.MediaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MediaServiceTest {

    @Mock
    private MediaRepository mediaRepo;

    @InjectMocks
    private MediaService mediaService;

    @Test
    void createMedia_ReturnsCreatedMedia() throws SQLException {
        // Arrange
        Media media = new Media();
        media.setTitle("Inception");

        Media createdMedia = new Media();
        createdMedia.setId(1);
        createdMedia.setTitle("Inception");

        when(mediaRepo.create(any(Media.class))).thenReturn(createdMedia);

        // Act
        Media result = mediaService.createMedia(media);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("Inception", result.getTitle());
    }

    @Test
    void updateMedia_SetsCreatedByToEnsureOwnership() throws SQLException {
        // Arrange
        Media media = new Media();
        media.setId(1);
        media.setTitle("Updated Title");

        when(mediaRepo.update(any(Media.class))).thenReturn(true);

        // Act
        boolean success = mediaService.updateMedia(media, 5); // User 5 macht das Update

        // Assert
        assertTrue(success);
        assertEquals(5, media.getCreatedBy()); // WICHTIG: Service muss die ID auf 5 setzen!
        verify(mediaRepo, times(1)).update(media);
    }

    @Test
    void deleteMedia_PassesUserIdToRepository() throws SQLException {
        // Arrange
        when(mediaRepo.delete(1, 5)).thenReturn(true);

        // Act
        boolean success = mediaService.deleteMedia(1, 5);

        // Assert
        assertTrue(success);
        verify(mediaRepo, times(1)).delete(1, 5);
    }

    @Test
    void searchAndFilter_PassesParamsToRepository() throws SQLException {
        // Arrange
        Map<String, String> params = new HashMap<>();
        params.put("genre", "sci-fi");

        Media media = new Media();
        media.setTitle("Matrix");
        when(mediaRepo.searchAndFilter(params)).thenReturn(Arrays.asList(media));

        // Act
        List<Media> result = mediaService.searchAndFilter(params);

        // Assert
        assertEquals(1, result.size());
        assertEquals("Matrix", result.get(0).getTitle());
    }

    @Test
    void addFavorite_ReturnsTrueOnSuccess() {
        // Arrange
        when(mediaRepo.addFavorite(1, 5)).thenReturn(true);

        // Act
        boolean success = mediaService.addFavorite(1, 5);

        // Assert
        assertTrue(success);
    }
}