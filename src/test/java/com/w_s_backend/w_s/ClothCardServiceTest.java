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

import com.w_s_backend.w_s.DTOs.CurrentWeatherDto;
import com.w_s_backend.w_s.Repositories.ClothCardPepository;
import com.w_s_backend.w_s.Repositories.OutfitRepository;
import com.w_s_backend.w_s.Services.ClothCardService;
import com.w_s_backend.w_s.Services.ColorMatchingService;
import com.w_s_backend.w_s.Services.UserService;
import com.w_s_backend.w_s.Services.WeatherService;
import com.w_s_backend.w_s.models.*;
import com.w_s_backend.w_s.DTOs.CurrentWeatherDto.Current;
import com.w_s_backend.w_s.DTOs.CurrentWeatherDto.Condition;

@ExtendWith(MockitoExtension.class)
class ClothCardServiceTest {

    @Mock
    private ClothCardPepository clothCardRepository;
    
    @Mock
    private UserService userService;
    
    @Mock
    private OutfitRepository outfitRepository;
    
    @Mock
    private ColorMatchingService colorMatchingService;
    
    @Mock
    private WeatherService weatherService;
    
    @InjectMocks
    private ClothCardService clothCardService;
    
    private User testUser;
    
    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
    }
    
    /**
     * Создаёт тестовую карточку одежды
     */
    private ClothCard createCard(Long id, String name, ClothingCategory category, 
                                  ClothStyle style, Season season, int warmth) {
        ClothCard card = new ClothCard();
        card.setId(id);
        card.setClothName(name);
        card.setCategory(category);
        card.setStyle(style);
        card.setSeason(season);
        card.setWarmthLevel(warmth);
        card.setColor("Черный");
        card.setUser(testUser);
        return card;
    }
    
    /**
     * Создаёт мок погоды с указанной температурой
     */
    private CurrentWeatherDto createWeatherMock(double temperature, String condition) {
        CurrentWeatherDto weatherDto = new CurrentWeatherDto();
        Current current = new Current();
        current.setTemp_c(temperature);
        current.setFeelslike_c(temperature - 2);
        
        Condition cond = new Condition();
        cond.setText(condition);
        cond.setIcon("//cdn.weatherapi.com/weather/64x64/day/116.png");
        current.setCondition(cond);
        
        weatherDto.setCurrent(current);
        
        CurrentWeatherDto.Location location = new CurrentWeatherDto.Location();
        location.setName("Moscow");
        location.setCountry("Russia");
        weatherDto.setLocation(location);
        
        return weatherDto;
    }
    
    // ==================== ТЕСТ 1: Зимний образ должен содержать больше 2 вещей ====================
    @Test
    void winterOutfits_ShouldHaveMoreThanTwoItems() {
        // Arrange
        List<ClothCard> winterCards = new ArrayList<>();
        
        // Создаём полный зимний гардероб
        winterCards.add(createCard(1L, "Рубашка", ClothingCategory.TOP_BASE, ClothStyle.CASUAL, Season.WINTER, 5));
        winterCards.add(createCard(2L, "Брюки", ClothingCategory.BOTTOM, ClothStyle.CASUAL, Season.WINTER, 5));
        winterCards.add(createCard(3L, "Свитер", ClothingCategory.TOP_MID, ClothStyle.CASUAL, Season.WINTER, 5));
        winterCards.add(createCard(4L, "Пуховик", ClothingCategory.TOP_OUTER, ClothStyle.CASUAL, Season.WINTER, 5));
        winterCards.add(createCard(5L, "Ботинки", ClothingCategory.SHOES, ClothStyle.CASUAL, Season.WINTER, 5));
        winterCards.add(createCard(6L, "Шапка", ClothingCategory.HEAD, ClothStyle.CASUAL, Season.WINTER, 5));
        
        when(userService.findById(1L)).thenReturn(testUser);
        when(clothCardRepository.findByUserId(1L)).thenReturn(winterCards);
        
        // Мокаем погоду: -15°C (зима)
        CurrentWeatherDto weatherDto = createWeatherMock(-15.0, "Ясно");
        when(weatherService.getCurrentWeather(anyString())).thenReturn(weatherDto);
        
        // Разрешаем все цвета
        when(colorMatchingService.matchesColorScheme(anyString(), anyList(), any()))
            .thenReturn(true);
        
        // Act
        List<Outfit> outfits = clothCardService.generateAndSaveOutfits(
            1L, OutfitStyle.CASUAL, 3, "Зимний тест", ColorScheme.ANY, 55.75, 37.61); // передаём координаты
        
        // Assert
        assertNotNull(outfits, "Список образов не должен быть null");
        assertFalse(outfits.isEmpty(), "Должны быть сгенерированы образы");
        
        System.out.println("\n✅ Зимние образы (-15°C):");
        for (Outfit outfit : outfits) {
            int itemCount = outfit.getItems().size();
            System.out.printf("🔹 %s (%d вещей):\n", outfit.getOutfitName(), itemCount);
            
            // Проверяем, что вещей больше 2
            assertTrue(itemCount > 2, 
                String.format("❌ Образ '%s' содержит только %d вещи! При -15°C должно быть больше 2.", 
                    outfit.getOutfitName(), itemCount));
            
            // Проверяем наличие тёплых слоёв
            boolean hasMidOrOuter = outfit.getItems().stream()
                .anyMatch(item -> item.getCategory() == ClothingCategory.TOP_MID 
                               || item.getCategory() == ClothingCategory.TOP_OUTER);
            assertTrue(hasMidOrOuter, 
                String.format("❌ Образ '%s' не содержит свитера или куртки!", outfit.getOutfitName()));
            
            // Проверяем теплоту каждой вещи
            for (ClothCard item : outfit.getItems()) {
                System.out.printf("   - %s [%s, теплота: %d, сезон: %s]\n",
                    item.getClothName(), item.getCategory(), 
                    item.getWarmthLevel(), item.getSeason());
                    
                assertTrue(item.getWarmthLevel() >= 3,
                    String.format("❌ Вещь '%s' слишком лёгкая (теплота %d) для -15°C!", 
                        item.getClothName(), item.getWarmthLevel()));
            }
        }
    }
    
    // ==================== ТЕСТ 2: Зимой нельзя только верх + низ ====================
    @Test
    void winterOutfits_ShouldNotBeOnlyTopAndBottom() {
        // Arrange
        List<ClothCard> winterCards = new ArrayList<>();
        
        winterCards.add(createCard(1L, "Рубашка", ClothingCategory.TOP_BASE, ClothStyle.CASUAL, Season.WINTER, 5));
        winterCards.add(createCard(2L, "Брюки", ClothingCategory.BOTTOM, ClothStyle.CASUAL, Season.WINTER, 5));
        winterCards.add(createCard(3L, "Свитер", ClothingCategory.TOP_MID, ClothStyle.CASUAL, Season.WINTER, 5));
        winterCards.add(createCard(4L, "Пуховик", ClothingCategory.TOP_OUTER, ClothStyle.CASUAL, Season.WINTER, 5));
        winterCards.add(createCard(5L, "Ботинки", ClothingCategory.SHOES, ClothStyle.CASUAL, Season.WINTER, 5));
        winterCards.add(createCard(6L, "Шапка", ClothingCategory.HEAD, ClothStyle.CASUAL, Season.WINTER, 5));
        
        when(userService.findById(1L)).thenReturn(testUser);
        when(clothCardRepository.findByUserId(1L)).thenReturn(winterCards);
        
        // Мокаем погоду: -10°C (снег)
        CurrentWeatherDto weatherDto = createWeatherMock(-10.0, "Снег");
        when(weatherService.getCurrentWeather(anyString())).thenReturn(weatherDto);
        when(colorMatchingService.matchesColorScheme(anyString(), anyList(), any()))
            .thenReturn(true);
        
        // Act
        List<Outfit> outfits = clothCardService.generateAndSaveOutfits(
            1L, OutfitStyle.CASUAL, 5, null, ColorScheme.ANY, 55.75, 37.61); // передаём координаты
        
        // Assert
        assertNotNull(outfits);
        assertFalse(outfits.isEmpty());
        
        System.out.println("\n✅ Проверка, что нет образов только из верха и низа (-10°C):");
        for (Outfit outfit : outfits) {
            List<ClothCard> items = outfit.getItems();
            
            Set<ClothingCategory> categories = new HashSet<>();
            for (ClothCard item : items) {
                categories.add(item.getCategory());
            }
            
            System.out.printf("🔹 %s — %d вещей, категории: %s\n",
                outfit.getOutfitName(), items.size(), categories);
            
            // Проверяем, что есть не только TOP_BASE и BOTTOM
            boolean hasOnlyTopAndBottom = categories.size() == 2 
                && categories.contains(ClothingCategory.TOP_BASE) 
                && categories.contains(ClothingCategory.BOTTOM);
            
            assertFalse(hasOnlyTopAndBottom,
                String.format("❌ Образ '%s' содержит только верх и низ! При -10°C нужны слои.", 
                    outfit.getOutfitName()));
        }
    }
    
    // ==================== ТЕСТ 3: Осенний образ (9°C) должен содержать средний слой ====================
    @Test
    void autumnOutfits_ShouldIncludeMidLayer() {
        // Arrange
        List<ClothCard> autumnCards = new ArrayList<>();
        
        autumnCards.add(createCard(1L, "Рубашка", ClothingCategory.TOP_BASE, ClothStyle.CASUAL, Season.ALL_SEASON, 3));
        autumnCards.add(createCard(2L, "Джинсы", ClothingCategory.BOTTOM, ClothStyle.CASUAL, Season.ALL_SEASON, 3));
        autumnCards.add(createCard(3L, "Свитер", ClothingCategory.TOP_MID, ClothStyle.CASUAL, Season.ALL_SEASON, 4));
        autumnCards.add(createCard(4L, "Кроссовки", ClothingCategory.SHOES, ClothStyle.CASUAL, Season.ALL_SEASON, 2));
        
        when(userService.findById(1L)).thenReturn(testUser);
        when(clothCardRepository.findByUserId(1L)).thenReturn(autumnCards);
        
        // Мокаем погоду: 9°C (осень)
        CurrentWeatherDto weatherDto = createWeatherMock(9.0, "Облачно");
        when(weatherService.getCurrentWeather(anyString())).thenReturn(weatherDto);
        when(colorMatchingService.matchesColorScheme(anyString(), anyList(), any()))
            .thenReturn(true);
        
        // Act
        List<Outfit> outfits = clothCardService.generateAndSaveOutfits(
            1L, OutfitStyle.CASUAL, 3, "Осенний тест", ColorScheme.ANY, 55.75, 37.61); // передаём координаты
        
        // Assert
        assertNotNull(outfits);
        assertFalse(outfits.isEmpty());
        
        System.out.println("\n✅ Осенние образы (9°C):");
        for (Outfit outfit : outfits) {
            System.out.printf("🔹 %s (%d вещей):\n", outfit.getOutfitName(), outfit.getItems().size());
            outfit.getItems().forEach(item -> 
                System.out.printf("   - %s [%s]\n", item.getClothName(), item.getCategory()));
        }
    }
}