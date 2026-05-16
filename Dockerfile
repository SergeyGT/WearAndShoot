# Этап 1: Сборка приложения
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

# Копируем все файлы проекта
COPY . .

# Даем права на выполнение mvnw (если используешь Maven Wrapper)
RUN chmod +x mvnw

# Собираем jar-файл (пропускаем тесты для ускорения)
RUN ./mvnw clean package -DskipTests

# Этап 2: Запуск приложения
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Копируем собранный jar из первого этапа
COPY --from=build /app/target/*.jar app.jar

# Открываем порт
EXPOSE 8080

# Запускаем приложение
ENTRYPOINT ["java", "-jar", "app.jar"]