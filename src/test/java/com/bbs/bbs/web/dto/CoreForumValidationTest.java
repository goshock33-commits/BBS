package com.bbs.bbs.web.dto;

import com.bbs.bbs.web.dto.admin.ModerationRequest;
import com.bbs.bbs.web.dto.comment.CreateCommentRequest;
import com.bbs.bbs.web.dto.post.CreatePostRequest;
import com.bbs.bbs.web.dto.post.PostPageRequest;
import com.bbs.bbs.web.error.ErrorCode;
import com.bbs.bbs.web.model.ContentStatus;
import com.bbs.bbs.web.response.PageResult;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoreForumValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validator = null;
    }

    @Test
    void rejectsPostTitleLongerThanThirtyTwoCharacters() {
        CreatePostRequest request = new CreatePostRequest(
                1, "x".repeat(33), "正文", null);

        assertHasViolation(request, "title");
    }

    @Test
    void rejectsPostBodyLongerThanOneHundredThousandCharacters() {
        CreatePostRequest request = new CreatePostRequest(
                1, "标题", "x".repeat(100_001), null);

        assertHasViolation(request, "content");
    }

    @Test
    void rejectsCommentLongerThanTwoHundredCharacters() {
        CreateCommentRequest request = new CreateCommentRequest(
                "x".repeat(201), null);

        assertHasViolation(request, "body");
    }

    @Test
    void rejectsInvalidCategoryAndDisabledCategoryErrorIsStable() {
        CreatePostRequest request = new CreatePostRequest(
                0, "标题", "正文", null);

        assertHasViolation(request, "categoryId");
        assertEquals("CATEGORY_DISABLED", ErrorCode.CATEGORY_DISABLED.name());
    }

    @Test
    void rejectsBlankModerationReason() {
        assertHasViolation(new ModerationRequest("   "), "reason");
    }

    @Test
    void rejectsPageSizeAboveFifty() {
        assertHasViolation(new PostPageRequest(1, 51, null, null, null, null), "size");
    }

    @Test
    void requestContractsDoNotAcceptActingUserId() {
        for (Class<?> requestType : Set.of(
                CreatePostRequest.class,
                CreateCommentRequest.class,
                ModerationRequest.class)) {
            Set<String> fields = Arrays.stream(requestType.getRecordComponents())
                    .map(component -> component.getName())
                    .collect(java.util.stream.Collectors.toSet());

            assertFalse(fields.contains("userId"));
            assertFalse(fields.contains("authorId"));
            assertFalse(fields.contains("operatorUserId"));
        }
    }

    @Test
    void contentStatusAllowsOnlySupportedTransitions() {
        assertTrue(ContentStatus.NORMAL.canTransitionTo(ContentStatus.HIDDEN));
        assertTrue(ContentStatus.HIDDEN.canTransitionTo(ContentStatus.NORMAL));
        assertTrue(ContentStatus.NORMAL.canTransitionTo(ContentStatus.DELETED));
        assertTrue(ContentStatus.HIDDEN.canTransitionTo(ContentStatus.DELETED));
        assertFalse(ContentStatus.NORMAL.canTransitionTo(ContentStatus.NORMAL));
        assertFalse(ContentStatus.DELETED.canTransitionTo(ContentStatus.NORMAL));
        assertFalse(ContentStatus.DELETED.canTransitionTo(ContentStatus.HIDDEN));
    }

    @Test
    void pageResultUsesStableGenericEnvelope() {
        PageResult<String> result = PageResult.of(List.of("first"), 11, 2, 10);

        assertEquals(List.of("first"), result.list());
        assertEquals(11, result.total());
        assertEquals(2, result.page());
        assertEquals(10, result.size());
        assertEquals(2, result.totalPages());

        PageResult<String> compatible = new PageResult<>(List.of(), 0, 1, 10);
        assertEquals(0, compatible.totalPages());
    }

    private void assertHasViolation(Object request, String property) {
        assertTrue(validator.validate(request).stream()
                        .anyMatch(error -> property.equals(error.getPropertyPath().toString())),
                () -> "Expected validation error on " + property);
    }
}
