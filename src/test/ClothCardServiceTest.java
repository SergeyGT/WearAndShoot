package com.w_s_backend.w_s.Services;

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
import com.w_s_backend.w_s.models.*;
import com.w_s_backend.w_s.models.CurrentWeatherDto.Current;

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
    private List<ClothCard> winterCards;
    
    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        
        // Создаём зимний гардероб
        winterCards = new ArrayList<>();
        
        // 1. Тёплая рубашка (верх)
        ClothCard shirt = createCard(1L, "Рубашка", ClothingCategory.TOP_BASE, 
            ClothStyle.CASUAL, Season.WINTER, 4);
        winterCards.add(shirt);
        
        // 2. Тёплые брюки (низ)
        ClothCard pants = createCard(2L, "Брюки", ClothingCategory.BOTTOM, 
            ClothStyle.CASUAL, Season.WINTER, 4);
        winterCards.add(pants);
        
        // 3. Свитер (средний слой)
        ClothCard sweater = createCard(3L, "Свитер", ClothingCategory.TOP_MID, 
            ClothStyle.CASUAL, Season.WINTER, 5);
        winterCards.add(sweater);
        
        // 4. Пуховик (верхняя одежда)
        ClothCard jacket = createCard(4L, "Пуховик", ClothingCategory.TOP_OUTER, 
            ClothStyle.CASUAL, Season.WINTER, 5);
        winterCards.add(jacket);
        
        // 5. Зимние ботинки
        ClothCard boots = createCard(5L, "Ботинки", ClothingCategory.SHOES, 
            ClothStyle.CASUAL, Season.WINTER, 5);
        winterCards.add(boots);
        
        // 6. Шапка
        ClothCard hat = createCard(6L, "Шапка", ClothingCategory.HEAD, 
            ClothStyle.CASUAL, Season.WINTER, 5);
        winterCards.add(hat);
    }
    
    @Test
    void winterOutfits_ShouldHaveMoreThanTwoItems() {
        // Arrange
        when(userService.findById(1L)).thenReturn(testUser);
        when(clothCardRepository.findByUserId(1L)).thenReturn(winterCards);
        
        // Мокаем погоду: -15°C (зима)
        CurrentWeatherDto weatherDto = new CurrentWeatherDto();
        Current current = new Current();
        current.setTemp_c(-15.0);
        current.setCondition(new CurrentWeatherDto.Condition());
        current.getCondition().setText("Ясно");
        weatherDto.setCurrent(current);
        when(weatherService.getCurrentWeather(anyString())).thenReturn(weatherDto);
        
        // Мокаем цветовую схему — разрешаем всё
        when(colorMatchingService.matchesColorScheme(anyString(), anyList(), any()))
            .thenReturn(true);
        
        // Act
        List<Outfit> outfits = clothCardService.generateAndSaveOutfits(
            1L, OutfitStyle.CASUAL, 3, "Тест", ColorScheme.ANY, null, null);
        
        // Assert
        assertNotNull(outfits);
        assertFalse(outfits.isEmpty(), "Должны быть сгенерированы образы");
        
        for (Outfit outfit : outfits) {
            int itemCount = outfit.getItems().size();
            assertTrue(itemCount > 2, 
                String.format("❌ Образ '%s' содержит только %d вещи! При -15°C должно быть больше 2 (слои + обувь).\nСостав: %s",
                    outfit.getOutfitName(),
                    itemCount,
                    outfit.getItems().stream()
                        .map(c -> c.getClothName() + "(" + c.getCategory() + ")")
                        .reduce((a, b) -> a + ", " + b).orElse("пусто")
                ));
            
            // Проверяем, что есть тёплые слои
            boolean hasMidOrOuter = outfit.getItems().stream()
                .anyMatch(item -> item.getCategory() == ClothingCategory.TOP_MID 
                               || item.getCategory() == ClothingCategory.TOP_OUTER);
            assertTrue(hasMidOrOuter, 
                String.format("❌ Образ '%s' не содержит среднего или верхнего слоя! При -15°C это недопустимо.",
                    outfit.getOutfitName()));
            
            // Проверяем, что все вещи соответствуют зиме
            for (ClothCard item : outfit.getItems()) {
                assertTrue(item.getWarmthLevel() >= 3,
                    String.format("❌ Вещь '%s' имеет теплоту %d — слишком легко для -15°C!",
                        item.getClothName(), item.getWarmthLevel()));
                
                // Проверяем, что нет летних вещей
                assertNotEquals(Season.SUMMER, item.getSeason(),
                    String.format("❌ Летняя вещь '%s' в зимнем образе!", item.getClothName()));
            }
        }
        
        // Вывод для отладки
        System.out.println("\n✅ Все образы соответствуют зимней погоде (-15°C):");
        for (Outfit outfit : outfits) {
            System.out.printf("🔹 %s (%d вещей):\n", outfit.getOutfitName(), outfit.getItems().size());
            outfit.getItems().forEach(item -> 
                System.out.printf("   - %s [%s, теплота: %d, сезон: %s]\n",
                    item.getClothName(), item.getCategory(), 
                    item.getWarmthLevel(), item.getSeason()));
        }
    }
    
    @Test
    void winterOutfits_ShouldNotBeOnlyTopAndBottom() {
        // Arrange
        when(userService.findById(1L)).thenReturn(testUser);
        when(clothCardRepository.findByUserId(1L)).thenReturn(winterCards);
        
        CurrentWeatherDto weatherDto = new CurrentWeatherDto();
        Current current = new Current();
        current.setTemp_c(-10.0);
        current.setCondition(new CurrentWeatherDto.Condition());
        current.getCondition().setText("Снег");
        weatherDto.setCurrent(current);
        when(weatherService.getCurrentWeather(anyString())).thenReturn(weatherDto);
        when(colorMatchingService.matchesColorScheme(anyString(), anyList(), any()))
            .thenReturn(true);
        
        // Act
        List<Outfit> outfits = clothCardService.generateAndSaveOutfits(
            1L, OutfitStyle.CASUAL, 5, null, ColorScheme.ANY, null, null);
        
        // Assert — проверяем КАЖДЫЙ образ
        for (Outfit outfit : outfits) {
            List<ClothCard> items = outfit.getItems();
            
            // Собираем категории в образе
            Set<ClothingCategory> categories = items.stream()
                .map(ClothCard::getCategory)
                .collect(java.util.stream.Collectors.toSet());
            
            // Проверяем, что есть не только TOP_BASE и BOTTOM
            boolean hasOnlyTopAndBottom = categories.size() == 2 
                && categories.contains(ClothingCategory.TOP_BASE) 
                && categories.contains(ClothingCategory.BOTTOM);
            
            assertFalse(hasOnlyTopAndBottom,
                String.format("❌ Образ '%s' содержит ТОЛЬКО верх и низ! При -10°C нужны дополнительные слои.",
                    outfit.getOutfitName()));
            
            System.out.printf("✅ Образ '%s' — %d вещей, категории: %s\n",
                outfit.getOutfitName(), items.size(), categories);
        }
    }
    
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
}   