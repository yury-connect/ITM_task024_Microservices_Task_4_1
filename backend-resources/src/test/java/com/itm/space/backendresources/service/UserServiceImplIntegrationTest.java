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

    private String createdUserId;


    @Autowired
    UserServiceImplIntegrationTest(Keycloak keycloak) {
        this.keycloak = keycloak;
    }


    @Nested
    class UserCreationTests {

        @Test
        @WithMockUser(roles = "MODERATOR")
        void shouldCreateUserSuccessfully_WhenUserDoesNotExist() throws Exception {

            // Создаем объект запроса для создания пользователя
            UserRequest userRequest = createValidUserRequest();

            mvc.perform(requestWithContent(post("/api/users"), userRequest))
                    .andExpect(status().isOk());

            createdUserId = findUserIdByUsername(userRequest.getUsername());
        }


        @Test
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

        //  Проверка создания пользователя с некорректным username:
        @Test
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

        // Проверка создания пользователя с невалидным email:
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

        // Проверка создания пользователя с коротким паролем:
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


    @Nested
    class UserRetrievalByIdTests {

        @Test
        @WithMockUser(roles = "MODERATOR")
        void shouldReturnUserDetails_WhenUserExists() throws Exception {

            UUID userId = UUID.fromString("60208bfd-25c0-49c6-8139-8059d997eeda");

            mvc.perform(requestToJson(get("/api/users/{id}", userId)))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "MODERATOR")
        void shouldReturnServerError_WhenUserIsNotFound() throws Exception {

            UUID userId =UUID.randomUUID();

            mvc.perform(requestToJson(get("/api/users/{id}", userId)))
                    .andExpect(status().is5xxServerError());
        }
    }


    // Удаление созданного 'user' после каждого теста
    @AfterEach
    public void deleteUserIfExists() {
        if (createdUserId != null) {
            keycloak.realm("ITM").users().get(createdUserId).remove();
        }
    }



    // *** services methods ***
    private UserRequest createValidUserRequest() {
        return new UserRequest(
                "username_", // String username
                "email_@example.com", // String email
                "password_", // String password
                "firstName_", // String firstName
                "lastName_" // String lastName
        );
    }


    private String findUserIdByUsername(String username) {
        return keycloak.realm("ITM")
                .users()
                .search(username)
                .get(0)
                .getId(); // сохр. ID созданного 'user'
    }
}

