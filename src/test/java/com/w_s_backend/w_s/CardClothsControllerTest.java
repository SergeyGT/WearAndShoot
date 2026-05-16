package com.w_s_backend.w_s;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import com.w_s_backend.w_s.DTOs.ClothCardDTO;
import com.w_s_backend.w_s.DTOs.ClothCardResponseDTO;
import com.w_s_backend.w_s.DTOs.OutfitGenerateRequest;
import com.w_s_backend.w_s.DTOs.OutfitResponse;
import com.w_s_backend.w_s.Services.ClothCardService;
import com.w_s_backend.w_s.Services.JwtService;
import com.w_s_backend.w_s.controllers.CardClothsController;
import com.w_s_backend.w_s.models.*;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

@ExtendWith(MockitoExtension.class)
class CardClothsControllerTest {

    @Mock
    private ClothCardService clothCardService;

    @Mock
    private JwtService jwtService;

    @Mock
    private HttpServletRequest httpRequest;

    @InjectMocks
    private CardClothsController controller;

    private User testUser;
    private ClothCard testCard;
    private Outfit testOutfit;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);

        testCard = new ClothCard();
        testCard.setId(10L);
        testCard.setClothName("Футболка");
        testCard.setCategory(ClothingCategory.TOP_BASE);
        testCard.setUser(testUser);
        testCard.setImagePath("uploads/test.jpg");

        testOutfit = Outfit.builder()
                .id(1L)
                .outfitName("Тестовый образ")
                .style(OutfitStyle.CASUAL)
                .user(testUser)
                .items(new ArrayList<>(List.of(testCard)))
                .isLiked(false)
                .temperatureC(20.0)
                .weatherCondition("Ясно")
                .build();
    }

    // ==================== createCard ====================

    @Test
    void createCard_ShouldReturnOkWithCardId() {
        ClothCardDTO dto = new ClothCardDTO();
        dto.setClothName("Футболка");
        dto.setUserId(1L);

        MockMultipartFile file = new MockMultipartFile(
                "image", "test.jpg", "image/jpeg", "test".getBytes());

        when(clothCardService.createCard(any(ClothCardDTO.class), any()))
                .thenReturn(testCard);

        ResponseEntity<ClothCardResponseDTO> response = controller.createCard(dto, file);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(10L, response.getBody().getId());
    }

    // ==================== read (getUserCards) ====================

    @Test
    void getUserCards_ShouldReturnList() {
        when(clothCardService.readAllCards(1L)).thenReturn(List.of(testCard));

        List<ClothCard> result = controller.read(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Футболка", result.get(0).getClothName());
    }

    @Test
    void getUserCards_ShouldReturnEmptyList_WhenNoCards() {
        when(clothCardService.readAllCards(1L)).thenReturn(Collections.emptyList());

        List<ClothCard> result = controller.read(1L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== editCard ====================

    @Test
    void editCard_ShouldReturnOk() {
        ClothCardDTO dto = new ClothCardDTO();
        dto.setUserId(1L);

        MockMultipartFile file = new MockMultipartFile(
                "image", "test.jpg", "image/jpeg", "test".getBytes());

        when(clothCardService.updateCard(eq(10L), any(ClothCardDTO.class), any()))
                .thenReturn(testCard);

        ResponseEntity<ClothCardResponseDTO> response = controller.editCard(10L, dto, file);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(10L, response.getBody().getId());
    }

    // ==================== deleteCard ====================

    @Test
    void deleteCard_ShouldReturnOk_WhenAuthorized() {
        Cookie jwtCookie = new Cookie("jwt", "valid.token.here");
        when(httpRequest.getCookies()).thenReturn(new Cookie[]{jwtCookie});
        when(jwtService.isTokenValid("valid.token.here")).thenReturn(true);
        when(jwtService.extractUserId("valid.token.here")).thenReturn(1L);
        doNothing().when(clothCardService).deleteCard(10L, 1L);

        ResponseEntity<?> response = controller.deleteCard(10L, httpRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("успешно удалена"));
    }

    @Test
    void deleteCard_ShouldReturnUnauthorized_WhenNoJwt() {
        when(httpRequest.getCookies()).thenReturn(null);

        ResponseEntity<?> response = controller.deleteCard(10L, httpRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void deleteCard_ShouldReturnBadRequest_WhenRuntimeException() {
        Cookie jwtCookie = new Cookie("jwt", "valid.token.here");
        when(httpRequest.getCookies()).thenReturn(new Cookie[]{jwtCookie});
        when(jwtService.isTokenValid("valid.token.here")).thenReturn(true);
        when(jwtService.extractUserId("valid.token.here")).thenReturn(1L);
        doThrow(new RuntimeException("Вещь не найдена"))
                .when(clothCardService).deleteCard(10L, 1L);

        ResponseEntity<?> response = controller.deleteCard(10L, httpRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    // ==================== deleteOutfit ====================

    @Test
    void deleteOutfit_ShouldReturnOk_WhenAuthorized() {
        Cookie jwtCookie = new Cookie("jwt", "valid.token.here");
        when(httpRequest.getCookies()).thenReturn(new Cookie[]{jwtCookie});
        when(jwtService.isTokenValid("valid.token.here")).thenReturn(true);
        when(jwtService.extractUserId("valid.token.here")).thenReturn(1L);
        doNothing().when(clothCardService).deleteOutfit(1L, 1L);

        ResponseEntity<?> response = controller.deleteOutfit(1L, httpRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void deleteOutfit_ShouldReturnUnauthorized_WhenNoJwt() {
        when(httpRequest.getCookies()).thenReturn(null);

        ResponseEntity<?> response = controller.deleteOutfit(1L, httpRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    // ==================== toggleLike ====================

    @Test
    void toggleLikeOutfit_ShouldReturnOk() {
        Map<String, Long> request = Map.of("userId", 1L);
        testOutfit.setIsLiked(true);

        when(clothCardService.toggleLikeOutfit(1L, 1L)).thenReturn(testOutfit);

        ResponseEntity<OutfitResponse> response = controller.toggleLikeOutfit(1L, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getIsLiked());
    }

    @Test
    void toggleLikeOutfit_ShouldReturnBadRequest_WhenNoUserId() {
        Map<String, Long> request = Collections.emptyMap();

        ResponseEntity<OutfitResponse> response = controller.toggleLikeOutfit(1L, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    // ==================== getLikedOutfits ====================

    @Test
    void getLikedOutfits_ShouldReturnList() {
        when(clothCardService.getLikedOutfits(1L)).thenReturn(List.of(testOutfit));

        ResponseEntity<List<OutfitResponse>> response = controller.getLikedOutfits(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getLikedOutfits_ShouldReturnEmptyList() {
        when(clothCardService.getLikedOutfits(1L)).thenReturn(Collections.emptyList());

        ResponseEntity<List<OutfitResponse>> response = controller.getLikedOutfits(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
    }

    // ==================== getUserOutfits ====================

    @Test
    void getUserOutfits_ShouldReturnList() {
        when(clothCardService.getUserOutfits(1L)).thenReturn(List.of(testOutfit));

        ResponseEntity<List<OutfitResponse>> response = controller.getUserOutfits(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    // ==================== generateOutfits ====================

    @Test
    void generateOutfits_ShouldReturnOk_WhenAuthorized() {
        Cookie jwtCookie = new Cookie("jwt", "valid.token.here");
        when(httpRequest.getCookies()).thenReturn(new Cookie[]{jwtCookie});
        when(httpRequest.getSession(false)).thenReturn(null);
        when(jwtService.isTokenValid("valid.token.here")).thenReturn(true);
        when(jwtService.extractUserId("valid.token.here")).thenReturn(1L);

        OutfitGenerateRequest request = new OutfitGenerateRequest();
        request.setStyle(OutfitStyle.CASUAL);
        request.setCount(3);
        request.setColorScheme(ColorScheme.ANY);
        request.setLat(55.75);
        request.setLon(37.61);

        when(clothCardService.generateAndSaveOutfits(
                eq(1L), eq(OutfitStyle.CASUAL), eq(3), isNull(), eq(ColorScheme.ANY), eq(55.75), eq(37.61)))
                .thenReturn(List.of(testOutfit));

        ResponseEntity<?> response = controller.generateOutfits(request, httpRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof List);
    }

    @Test
    void generateOutfits_ShouldReturnUnauthorized_WhenNoJwt() {
        when(httpRequest.getCookies()).thenReturn(null);
        when(httpRequest.getSession(false)).thenReturn(null);

        OutfitGenerateRequest request = new OutfitGenerateRequest();

        ResponseEntity<?> response = controller.generateOutfits(request, httpRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void generateOutfits_ShouldReturnBadRequest_WhenIllegalState() {
        Cookie jwtCookie = new Cookie("jwt", "valid.token.here");
        when(httpRequest.getCookies()).thenReturn(new Cookie[]{jwtCookie});
        when(httpRequest.getSession(false)).thenReturn(null);
        when(jwtService.isTokenValid("valid.token.here")).thenReturn(true);
        when(jwtService.extractUserId("valid.token.here")).thenReturn(1L);

        OutfitGenerateRequest request = new OutfitGenerateRequest();
        request.setStyle(OutfitStyle.CASUAL);
        request.setCount(3);

        when(clothCardService.generateAndSaveOutfits(any(), any(), anyInt(), any(), any(), any(), any()))
                .thenThrow(new IllegalStateException("Недостаточно вещей"));

        ResponseEntity<?> response = controller.generateOutfits(request, httpRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}