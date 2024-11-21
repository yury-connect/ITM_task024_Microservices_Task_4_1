package com.itm.space.backendresources.service;

import com.itm.space.backendresources.BaseIntegrationTest;
import com.itm.space.backendresources.api.request.UserRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
class UserServiceImplIntegrationTest extends BaseIntegrationTest {

    private final Keycloak keycloak;

    private String createdUserId; // ID созданного в очередном методе юзера, нужен для его удаления перед запуском последующего теста



    @Autowired
    UserServiceImplIntegrationTest(Keycloak keycloak) {
        this.keycloak = keycloak;
    }



    // *** Группа тестов для создания пользователей ***
    @Nested
    class UserCreationTests {

        /**
         * Проверяет успешное создание пользователя, если пользователя еще не существует в базе данных.
         */ @Test
        @WithMockUser(roles = "MODERATOR")
        void shouldCreateUserSuccessfully_WhenUserDoesNotExist() throws Exception {

            // Создаем объект запроса для создания пользователя
            UserRequest userRequest = createValidUserRequest();

            mvc.perform(requestWithContent(post("/api/users"), userRequest))
                    .andExpect(status().isOk());

            createdUserId = findUserIdByUsername(userRequest.getUsername());
        }


        /**
         * Проверяет, что при попытке создать дублирующего пользователя
         * (с тем же именем) возвращается статус 409 Conflict.
         */  @Test
        @WithMockUser(roles = "MODERATOR")
        void shouldReturnConflictStatus_WhenTryingToCreateDuplicateUser() throws Exception {

            // Создаем объект запроса для создания пользователя
            UserRequest userRequest = createValidUserRequest();

            // Сначала создаем пользователя
            mvc.perform(requestWithContent(post("/api/users"), userRequest))
                    .andExpect(status().isOk());

            // Теперь повторяем запрос, чтобы создать пользователя с теми же данными, что уже есть
            mvc.perform(requestWithContent(post("/api/users"), userRequest))
                    .andDo(print())
                    .andExpect(status().isConflict()); // Проверяем, что возвращается статус 409 Conflict

            createdUserId = findUserIdByUsername(userRequest.getUsername());
        }


        /**
         * Проверяет, что создание пользователя с некорректным именем возвращает статус 400 Bad Request.
         */        @Test
        @WithMockUser(roles = "MODERATOR")
        void shouldReturnBadRequest_WhenUsernameIsInvalid() throws Exception {
            // Невалидные значения для username, которые мы хотим проверить
            List<String> invalidUsernames = List.of(
                    "",
                    " ",
                    "username_that_is_way_too_long_for_this_field"
            );

            for (String username : invalidUsernames) {
                UserRequest userRequest = new UserRequest(
                        username, // Невалидный username
                        "email_@example.com",
                        "password_",
                        "firstName_",
                        "lastName_"
                );

                mvc.perform(requestWithContent(post("/api/users"), userRequest))
                        .andExpect(status().isBadRequest())
                        .andDo(print()); // Для отладки выводим ответ, если тест не прошел
            }
        }


        /**
         * Проверяет, что создание пользователя с некорректным email возвращает статус 400 Bad Request.
         */
        @Test
        @WithMockUser(roles = "MODERATOR")
        void shouldReturnBadRequest_WhenEmailIsInvalid() throws Exception {
            // Невалидные email-адреса для проверки
            List<String> invalidEmails = List.of(
                    "invalid-email",
                    "",
                    "plainaddress",
                    "@missingusername.com",
                    "username@.com"
            );

            for (String email : invalidEmails) {
                UserRequest userRequest = new UserRequest(
                        "username_",
                        email, // Невалидный email
                        "password_",
                        "firstName_",
                        "lastName_"
                );

                mvc.perform(requestWithContent(post("/api/users"), userRequest))
                        .andExpect(status().isBadRequest())
                        .andDo(print()); // Для отладки можно добавить вывод ответа
            }
        }

        /**
         * Проверяет, что создание пользователя с коротким паролем возвращает статус 400 Bad Request.
         */
        @Test
        @WithMockUser(roles = "MODERATOR")
        void shouldReturnBadRequest_WhenPasswordIsTooShort() throws Exception {
            // Невалидные пароли, которые слишком короткие для проверки
            List<String> shortPasswords = List.of("pas", "12", "a", "");

            for (String password : shortPasswords) {
                UserRequest userRequest = new UserRequest(
                        "username_",
                        "email_@example.com",
                        password, // Короткий пароль
                        "firstName_",
                        "lastName_"
                );

                mvc.perform(requestWithContent(post("/api/users"), userRequest))
                        .andExpect(status().isBadRequest())
                        .andDo(print()); // Для отладки можно добавить вывод ответа
            }
        }
    }


    // *** Группа тестов для получения пользователей по ID ***
    @Nested
    class UserRetrievalByIdTests {

        /**
         * Проверяет, что информация о существующем пользователе корректно возвращается.
         */
        @Test
        @WithMockUser(roles = "MODERATOR")
        void shouldReturnUserDetails_WhenUserExists() throws Exception {
            UUID userId = UUID.fromString("60208bfd-25c0-49c6-8139-8059d997eeda");

            // Выполняем GET-запрос на получение пользователя по ID
            mvc.perform(requestToJson(get("/api/users/{id}", userId)))
                    .andExpect(status().isOk()); // Ожидаем статус 200 OK
        }

        /**
         * Проверяет, что при запросе несуществующего пользователя возвращается ошибка сервера (статус 5xx).
         */
        @Test
        @WithMockUser(roles = "MODERATOR")
        void shouldReturnServerError_WhenUserIsNotFound() throws Exception {
            UUID userId = UUID.randomUUID(); // Генерируем случайный UUID, который, скорее всего, не существует

            // Выполняем GET-запрос и проверяем, что возвращается статус ошибки сервера (5xx)
            mvc.perform(requestToJson(get("/api/users/{id}", userId)))
                    .andExpect(status().is5xxServerError());
        }
    }


    // *** Удаление созданного пользователя после каждого теста ***
    @AfterEach
    public void deleteUserIfExists() {
        if (createdUserId != null) {
            keycloak.realm("ITM").users().get(createdUserId).remove();
        }
    }



    // *** Сервисные методы для создания запроса и поиска пользователей ***

    /**
     * Создает и возвращает объект запроса для создания нового пользователя с корректными значениями.
     */
    private UserRequest createValidUserRequest() {
        return new UserRequest(
                "username_", // String username
                "email_@example.com", // String email
                "password_", // String password
                "firstName_", // String firstName
                "lastName_" // String lastName
        );
    }


    /**
     * Находит и возвращает ID пользователя по его имени пользователя, используя Keycloak.
     */
    private String findUserIdByUsername(String username) {
        return keycloak.realm("ITM")
                .users()
                .search(username)
                .get(0)
                .getId(); // сохр. ID созданного 'user'
    }
}
