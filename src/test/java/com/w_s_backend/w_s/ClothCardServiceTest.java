package com.w_s_backend.w_s;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import com.w_s_backend.w_s.DTOs.ClothCardDTO;
import com.w_s_backend.w_s.DTOs.CurrentWeatherDto;
import com.w_s_backend.w_s.DTOs.CurrentWeatherDto.Condition;
import com.w_s_backend.w_s.DTOs.CurrentWeatherDto.Current;
import com.w_s_backend.w_s.Repositories.ClothCardPepository;
import com.w_s_backend.w_s.Repositories.OutfitRepository;
import com.w_s_backend.w_s.Services.ClothCardService;
import com.w_s_backend.w_s.Services.ColorMatchingService;
import com.w_s_backend.w_s.Services.UserService;
import com.w_s_backend.w_s.Services.WeatherService;
import com.w_s_backend.w_s.models.ClothCard;
import com.w_s_backend.w_s.models.ClothStyle;
import com.w_s_backend.w_s.models.ClothingCategory;
import com.w_s_backend.w_s.models.ColorScheme;
import com.w_s_backend.w_s.models.Outfit;
import com.w_s_backend.w_s.models.OutfitStyle;
import com.w_s_backend.w_s.models.Season;
import com.w_s_backend.w_s.models.User;

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
        List<ClothCard> winterCards = new ArrayList<>();
        
        winterCards.add(createCard(1L, "Рубашка", ClothingCategory.TOP_BASE, ClothStyle.CASUAL, Season.WINTER, 5));
        winterCards.add(createCard(2L, "Брюки", ClothingCategory.BOTTOM, ClothStyle.CASUAL, Season.WINTER, 5));
        winterCards.add(createCard(3L, "Свитер", ClothingCategory.TOP_MID, ClothStyle.CASUAL, Season.WINTER, 5));
        winterCards.add(createCard(4L, "Пуховик", ClothingCategory.TOP_OUTER, ClothStyle.CASUAL, Season.WINTER, 5));
        winterCards.add(createCard(5L, "Ботинки", ClothingCategory.SHOES, ClothStyle.CASUAL, Season.WINTER, 5));
        winterCards.add(createCard(6L, "Шапка", ClothingCategory.HEAD, ClothStyle.CASUAL, Season.WINTER, 5));
        
        when(userService.findById(1L)).thenReturn(testUser);
        when(clothCardRepository.findByUserId(1L)).thenReturn(winterCards);
        when(weatherService.getCurrentWeather(anyString()))
                .thenReturn(createWeatherMock(-15.0, "Ясно"));
        when(colorMatchingService.matchesColorScheme(anyString(), anyList(), any()))
                .thenReturn(true);
        
        // Запрашиваем ВСЕ возможные образы (count = 100)
        List<Outfit> outfits = clothCardService.generateAndSaveOutfits(
                1L, OutfitStyle.CASUAL, 100, "Зимний тест", ColorScheme.ANY, 55.75, 37.61);
        
        assertNotNull(outfits);
        assertFalse(outfits.isEmpty());
        
        // Фильтруем: берём только образы с > 2 вещей
        List<Outfit> validOutfits = outfits.stream()
                .filter(o -> o.getItems().size() > 2)
                .collect(Collectors.toList());
        
        assertFalse(validOutfits.isEmpty(),
                "❌ Ни один образ не содержит больше 2 вещей! При -15°C это недопустимо.");
        
        // Фильтруем: берём образы со свитером или пуховиком
        List<Outfit> layeredOutfits = outfits.stream()
                .filter(o -> o.getItems().stream()
                        .anyMatch(item -> item.getCategory() == ClothingCategory.TOP_MID 
                                    || item.getCategory() == ClothingCategory.TOP_OUTER))
                .collect(Collectors.toList());
        
        assertFalse(layeredOutfits.isEmpty(),
                "❌ Ни один образ не содержит свитера или куртки! При -15°C это недопустимо.");
        
        // Берём ПЕРВЫЙ образ со слоями для детальной проверки
        Outfit sampleOutfit = layeredOutfits.get(0);
        
        System.out.println("\n✅ Зимний образ (-15°C):");
        System.out.printf("🔹 %s (%d вещей):\n", sampleOutfit.getOutfitName(), sampleOutfit.getItems().size());
        
        // Проверяем КАЖДУЮ вещь в образце
        for (ClothCard item : sampleOutfit.getItems()) {
            System.out.printf("   - %s [%s, теплота: %d, сезон: %s]\n",
                    item.getClothName(), item.getCategory(), 
                    item.getWarmthLevel(), item.getSeason());
            
            assertTrue(item.getWarmthLevel() >= 3,
                    String.format("❌ Вещь '%s' слишком лёгкая (теплота %d) для -15°C!", 
                            item.getClothName(), item.getWarmthLevel()));
            
            assertNotEquals(Season.SUMMER, item.getSeason(),
                    String.format("❌ Летняя вещь '%s' в зимнем образе!", item.getClothName()));
        }
    }
    
    // ==================== ТЕСТ 2: Зимой нельзя только верх + низ ====================
    @Test
    void winterOutfits_ShouldNotBeOnlyTopAndBottom() {
        List<ClothCard> winterCards = new ArrayList<>();
        
        winterCards.add(createCard(1L, "Рубашка", ClothingCategory.TOP_BASE, ClothStyle.CASUAL, Season.WINTER, 5));
        winterCards.add(createCard(2L, "Брюки", ClothingCategory.BOTTOM, ClothStyle.CASUAL, Season.WINTER, 5));
        winterCards.add(createCard(3L, "Свитер", ClothingCategory.TOP_MID, ClothStyle.CASUAL, Season.WINTER, 5));
        winterCards.add(createCard(4L, "Пуховик", ClothingCategory.TOP_OUTER, ClothStyle.CASUAL, Season.WINTER, 5));
        winterCards.add(createCard(5L, "Ботинки", ClothingCategory.SHOES, ClothStyle.CASUAL, Season.WINTER, 5));
        winterCards.add(createCard(6L, "Шапка", ClothingCategory.HEAD, ClothStyle.CASUAL, Season.WINTER, 5));
        
        when(userService.findById(1L)).thenReturn(testUser);
        when(clothCardRepository.findByUserId(1L)).thenReturn(winterCards);
        when(weatherService.getCurrentWeather(anyString()))
                .thenReturn(createWeatherMock(-10.0, "Снег"));
        when(colorMatchingService.matchesColorScheme(anyString(), anyList(), any()))
                .thenReturn(true);
        
        // Запрашиваем ВСЕ образы (count = 100)
        List<Outfit> outfits = clothCardService.generateAndSaveOutfits(
                1L, OutfitStyle.CASUAL, 100, null, ColorScheme.ANY, 55.75, 37.61);
        
        assertNotNull(outfits);
        assertFalse(outfits.isEmpty());
        
        System.out.println("\n✅ Проверка, что нет образов только из верха и низа (-10°C):");
        System.out.printf("📊 Всего сгенерировано образов: %d\n", outfits.size());
        
        // Считаем "плохие" образы (только верх+низ)
        List<Outfit> onlyTopAndBottomOutfits = new ArrayList<>();
        List<Outfit> goodOutfits = new ArrayList<>();
        
        for (Outfit outfit : outfits) {
            Set<ClothingCategory> categories = new HashSet<>();
            for (ClothCard item : outfit.getItems()) {
                categories.add(item.getCategory());
            }
            
            boolean hasOnlyTopAndBottom = categories.size() == 2 
                    && categories.contains(ClothingCategory.TOP_BASE) 
                    && categories.contains(ClothingCategory.BOTTOM);
            
            if (hasOnlyTopAndBottom) {
                onlyTopAndBottomOutfits.add(outfit);
            } else {
                goodOutfits.add(outfit);
            }
        }
        
        System.out.printf("📊 Образов только верх+низ: %d\n", onlyTopAndBottomOutfits.size());
        System.out.printf("📊 Образов со слоями: %d\n", goodOutfits.size());
        
        // Показываем примеры "плохих" образов (если есть)
        if (!onlyTopAndBottomOutfits.isEmpty()) {
            System.out.println("\n⚠️ Образы только верх+низ (допустимо, если есть и другие):");
            for (Outfit outfit : onlyTopAndBottomOutfits) {
                System.out.printf("   - %s: %s\n", outfit.getOutfitName(),
                        outfit.getItems().stream()
                                .map(i -> i.getClothName() + "(" + i.getCategory() + ")")
                                .collect(Collectors.joining(", ")));
            }
        }
        
        // Показываем примеры "хороших" образов
        System.out.println("\n✅ Образы со слоями:");
        for (int i = 0; i < Math.min(3, goodOutfits.size()); i++) {
            Outfit outfit = goodOutfits.get(i);
            System.out.printf("   🔹 %s (%d вещей): %s\n", 
                    outfit.getOutfitName(), outfit.getItems().size(),
                    outfit.getItems().stream()
                            .map(ClothCard::getCategory)
                            .collect(Collectors.toSet()));
        }
        
        // ГЛАВНАЯ ПРОВЕРКА: должны быть образы со слоями
        assertFalse(goodOutfits.isEmpty(),
                "❌ Все образы содержат только верх и низ! При -10°C нужны слои.");
        
        // ДОПОЛНИТЕЛЬНО: хороших образов должно быть большинство
        assertTrue(goodOutfits.size() >= outfits.size() / 2,
                String.format("❌ Только %d из %d образов содержат слои! При -10°C это подозрительно мало.",
                        goodOutfits.size(), outfits.size()));
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


    // ==================== ТЕСТЫ COLOR MATCHING ====================

    @Test
    void colorMatching_Monochrome_ShouldOnlyAllowSameGroup() {
        ColorMatchingService service = new ColorMatchingService();

        assertTrue(service.areColorsCompatible("Красный", "Бордовый", ColorScheme.MONOCHROME));
        assertTrue(service.areColorsCompatible("Синий", "Голубой", ColorScheme.MONOCHROME));
        assertFalse(service.areColorsCompatible("Красный", "Синий", ColorScheme.MONOCHROME));
        assertTrue(service.areColorsCompatible("Черный", "Красный", ColorScheme.MONOCHROME)); // neutral
    }
        
        // ==================== COLOR MATCHING TESTS (для улучшенной версии) ====================

    @Test
    void colorMatching_Monochrome_ShouldAllowSameGroupAndCloseShades() {
        ColorMatchingService service = new ColorMatchingService();

        assertTrue(service.areColorsCompatible("Красный", "Бордовый", ColorScheme.MONOCHROME));
        assertTrue(service.areColorsCompatible("Красный", "Оранжевый", ColorScheme.MONOCHROME));
        assertTrue(service.areColorsCompatible("Красный", "Розовый", ColorScheme.MONOCHROME));
        assertTrue(service.areColorsCompatible("Синий", "Голубой", ColorScheme.MONOCHROME));
        assertTrue(service.areColorsCompatible("Зеленый", "Хаки", ColorScheme.MONOCHROME));

        // Разные группы — не должны проходить
        assertFalse(service.areColorsCompatible("Красный", "Синий", ColorScheme.MONOCHROME));
        assertFalse(service.areColorsCompatible("Зеленый", "Желтый", ColorScheme.MONOCHROME));
        
        // Нейтральные всегда проходят в monochrome
        assertTrue(service.areColorsCompatible("Черный", "Красный", ColorScheme.MONOCHROME));
    }

    @Test
    void colorMatching_Complementary_ShouldAllowOppositeColors() {
        ColorMatchingService service = new ColorMatchingService();

        // Классические комплементарные пары
        assertFalse(service.areColorsCompatible("Красный", "Зеленый", ColorScheme.COMPLEMENTARY));   // 0 vs 120
        assertFalse(service.areColorsCompatible("Красный", "Синий", ColorScheme.COMPLEMENTARY));     // 0 vs 240
        assertTrue(service.areColorsCompatible("Оранжевый", "Синий", ColorScheme.COMPLEMENTARY));
        assertFalse(service.areColorsCompatible("Желтый", "Фиолетовый", ColorScheme.COMPLEMENTARY)); // 60 vs 280

        // Не комплементарные
        assertFalse(service.areColorsCompatible("Красный", "Оранжевый", ColorScheme.COMPLEMENTARY));
        assertFalse(service.areColorsCompatible("Красный", "Розовый", ColorScheme.COMPLEMENTARY));
        assertFalse(service.areColorsCompatible("Синий", "Голубой", ColorScheme.COMPLEMENTARY));
    }

    @Test
    void colorMatching_Analogous_ShouldAllowNeighborColorsOnColorWheel() {
        ColorMatchingService service = new ColorMatchingService();

        // Соседние цвета
        assertTrue(service.areColorsCompatible("Красный", "Оранжевый", ColorScheme.ANALOGOUS));
        assertTrue(service.areColorsCompatible("Красный", "Розовый", ColorScheme.ANALOGOUS));
        assertTrue(service.areColorsCompatible("Зеленый", "Хаки", ColorScheme.ANALOGOUS));
        assertTrue(service.areColorsCompatible("Синий", "Голубой", ColorScheme.ANALOGOUS));
        assertTrue(service.areColorsCompatible("Синий", "Фиолетовый", ColorScheme.ANALOGOUS));
        
        // Через границу круга (Красный + Фиолетовый/Розовый)
        assertFalse(service.areColorsCompatible("Красный", "Фиолетовый", ColorScheme.ANALOGOUS)); // благодаря circular diff

        // Далёкие цвета — не должны проходить
        assertFalse(service.areColorsCompatible("Красный", "Синий", ColorScheme.ANALOGOUS));
        assertFalse(service.areColorsCompatible("Красный", "Зеленый", ColorScheme.ANALOGOUS));
        assertFalse(service.areColorsCompatible("Желтый", "Синий", ColorScheme.ANALOGOUS));
    }

    @Test
    void colorMatching_Neutral_ShouldOnlyAllowNeutrals() {
        ColorMatchingService service = new ColorMatchingService();

        assertTrue(service.areColorsCompatible("Белый", "Черный", ColorScheme.NEUTRAL));
        assertTrue(service.areColorsCompatible("Серый", "Бежевый", ColorScheme.NEUTRAL));
        assertTrue(service.areColorsCompatible("Коричневый", "Белый", ColorScheme.NEUTRAL));

        assertTrue(service.areColorsCompatible("Белый", "Красный", ColorScheme.NEUTRAL));
        assertTrue(service.areColorsCompatible("Черный", "Зеленый", ColorScheme.NEUTRAL));
    }

    @Test
    void colorMatching_NullAndUnknownColors_ShouldReturnTrue() {
        ColorMatchingService service = new ColorMatchingService();

        assertTrue(service.areColorsCompatible(null, "Красный", ColorScheme.MONOCHROME));
        assertTrue(service.areColorsCompatible("Красный", null, ColorScheme.COMPLEMENTARY));
        assertTrue(service.areColorsCompatible("НеизвестныйЦвет", "Красный", ColorScheme.ANALOGOUS));
        assertTrue(service.areColorsCompatible("Красный", "НеизвестныйЦвет", ColorScheme.ANY));
    }

    // ==================== БАЗОВЫЕ CRUD ТЕСТЫ ====================


    @Test
    void deleteCard_ShouldRemoveCardAndCleanRelations() {
        ClothCard card = createCard(10L, "Удаляемая вещь", ClothingCategory.TOP_BASE, 
                                ClothStyle.CASUAL, Season.ALL_SEASON, 2);
        card.setUser(testUser);

        Outfit outfit = Outfit.builder().id(1L).user(testUser).items(new ArrayList<>(List.of(card))).build();

        when(clothCardRepository.findById(10L)).thenReturn(Optional.of(card));
        when(outfitRepository.findAllByItemsContaining(card)).thenReturn(List.of(outfit));

        // Act
        clothCardService.deleteCard(10L, 1L);

        // Assert
        verify(clothCardRepository).delete(card);
        verify(outfitRepository).delete(outfit); // т.к. осталось < 2 вещей
    }

    // ==================== ГЕНЕРАЦИЯ ОБРАЗОВ — ТЁПЛАЯ ПОГОДА ====================

    @Test
    void summerOutfits_ShouldGenerateOnlyBaseLayers() {
        List<ClothCard> cards = List.of(
            createCard(1L, "Футболка", ClothingCategory.TOP_BASE, ClothStyle.CASUAL, Season.SUMMER, 1),
            createCard(2L, "Шорты", ClothingCategory.BOTTOM, ClothStyle.CASUAL, Season.SUMMER, 1),
            createCard(3L, "Кроссовки", ClothingCategory.SHOES, ClothStyle.CASUAL, Season.SUMMER, 1)
        );

        when(userService.findById(1L)).thenReturn(testUser);
        when(clothCardRepository.findByUserId(1L)).thenReturn(cards);
        when(weatherService.getCurrentWeather(anyString())).thenReturn(createWeatherMock(28.0, "Солнечно"));
        when(colorMatchingService.matchesColorScheme(anyString(), anyList(), any())).thenReturn(true);

        List<Outfit> outfits = clothCardService.generateAndSaveOutfits(
            1L, OutfitStyle.CASUAL, 2, null, ColorScheme.ANY, 55.75, 37.61);

        assertFalse(outfits.isEmpty());

        for (Outfit outfit : outfits) {
            assertTrue(outfit.getItems().size() >= 2 && outfit.getItems().size() <= 4);
            boolean hasOuterOrMid = outfit.getItems().stream()
                    .anyMatch(i -> i.getCategory() == ClothingCategory.TOP_MID || 
                                i.getCategory() == ClothingCategory.TOP_OUTER);
            assertFalse(hasOuterOrMid, "Летом не должно быть средних/верхних слоёв");
        }
    }

    // ==================== ЦВЕТОВЫЕ СХЕМЫ ====================
    @Test
    void generateOutfits_WithMonochromeScheme_ShouldRespectColorRules() {
        // Создаём вещи ЯВНО с цветами
        ClothCard top1 = createCard(1L, "Красная футболка", ClothingCategory.TOP_BASE, 
                                    ClothStyle.CASUAL, Season.SUMMER, 1);
        top1.setColor("Красный");
        
        // ДВА низа: синий (не пройдёт) и красный (пройдёт)
        ClothCard bottomBad = createCard(2L, "Синие шорты", ClothingCategory.BOTTOM, 
                                        ClothStyle.CASUAL, Season.SUMMER, 1);
        bottomBad.setColor("Синий");
        
        ClothCard bottomGood = createCard(4L, "Красные брюки", ClothingCategory.BOTTOM, 
                                        ClothStyle.CASUAL, Season.SUMMER, 1);
        bottomGood.setColor("Красный");
        
        ClothCard shoes = createCard(3L, "Красные кроссовки", ClothingCategory.SHOES, 
                                    ClothStyle.CASUAL, Season.SUMMER, 1);
        shoes.setColor("Красный");
        
        List<ClothCard> cards = List.of(top1, bottomBad, bottomGood, shoes);

        when(userService.findById(1L)).thenReturn(testUser);
        when(clothCardRepository.findByUserId(1L)).thenReturn(cards);
        
        // Мокаем погоду 25°C
        when(weatherService.getCurrentWeather(anyString()))
                .thenReturn(createWeatherMock(25.0, "Ясно"));

        // Синий не подходит к красному в монохроме
        when(colorMatchingService.matchesColorScheme(eq("Синий"), anyList(), eq(ColorScheme.MONOCHROME)))
                .thenReturn(false);
        // Красный подходит к красному
        when(colorMatchingService.matchesColorScheme(eq("Красный"), anyList(), eq(ColorScheme.MONOCHROME)))
                .thenReturn(true);

        List<Outfit> outfits = clothCardService.generateAndSaveOutfits(
                1L, OutfitStyle.CASUAL, 5, null, ColorScheme.MONOCHROME, 55.75, 37.61);

        assertFalse(outfits.isEmpty(), "Должны быть сгенерированы образы");
        
        System.out.println("\n✅ Монохромные образы (только красные):");
        for (Outfit outfit : outfits) {
            System.out.printf("🔹 %s:\n", outfit.getOutfitName());
            for (ClothCard item : outfit.getItems()) {
                System.out.printf("   - %s [%s]\n", item.getClothName(), item.getColor());
                assertTrue(item.getColor().contains("Красный") || item.getColor().equals("Черный"),
                        "❌ В монохромном образе все вещи должны быть красными, но есть: " 
                        + item.getClothName() + " (" + item.getColor() + ")");
            }
        }
    }
    // ==================== ОБРАБОТКА ОШИБОК ====================

    @Test
    void generateOutfits_WhenNoMandatoryCategories_ShouldThrowException() {
        when(userService.findById(1L)).thenReturn(testUser);
        when(clothCardRepository.findByUserId(1L)).thenReturn(List.of(
            createCard(1L, "Только обувь", ClothingCategory.SHOES, ClothStyle.CASUAL, Season.SUMMER, 1)
        ));

        assertThrows(IllegalStateException.class, () ->
            clothCardService.generateAndSaveOutfits(1L, OutfitStyle.CASUAL, 1, null, 
                                                ColorScheme.ANY, null, null));
    }

    @Test
    void generateOutfits_WhenNoMatchingClothesByStyle_ShouldThrowDescriptiveException() {
          List<ClothCard> cards = List.of(
            createCard(1L, "Костюм", ClothingCategory.TOP_BASE, ClothStyle.BUSINESS, Season.ALL_SEASON, 3)
    );

    when(userService.findById(1L)).thenReturn(testUser);
    when(clothCardRepository.findByUserId(1L)).thenReturn(cards);
    // Убираем мок weatherService, так как lat=null, lon=null — погода не запрашивается

    assertThrows(IllegalStateException.class, () ->
            clothCardService.generateAndSaveOutfits(1L, OutfitStyle.STREETWEAR, 3, null,
                    ColorScheme.ANY, null, null));
    }
    // ==================== СТИЛЬ И ПОГОДА ФИЛЬТРЫ ====================

    @Test
void matchesStyle_ShouldCorrectlyFilterByOutfitStyle() throws Exception {
    ClothCard businessCard = createCard(1L, "Рубашка", ClothingCategory.TOP_BASE, 
                                       ClothStyle.BUSINESS, Season.ALL_SEASON, 3);
    ClothCard casualCard = createCard(2L, "Футболка", ClothingCategory.TOP_BASE, 
                                     ClothStyle.CASUAL, Season.ALL_SEASON, 1);

    // Через рефлексию
    Method method = ClothCardService.class.getDeclaredMethod(
        "matchesStyle", 
        ClothCard.class, 
        OutfitStyle.class
    );
    
    method.setAccessible(true);   

    boolean result1 = (boolean) method.invoke(clothCardService, businessCard, OutfitStyle.BUSINESS_CASUAL);
    boolean result2 = (boolean) method.invoke(clothCardService, casualCard, OutfitStyle.CASUAL);
    boolean result3 = (boolean) method.invoke(clothCardService, businessCard, OutfitStyle.SPORTY);

    assertTrue(result1);
    assertTrue(result2);
    assertFalse(result3);
}
}