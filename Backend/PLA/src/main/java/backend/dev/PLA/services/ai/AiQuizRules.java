package backend.dev.PLA.services.ai;

import java.util.List;

/** Hằng số và helper dùng chung giữa prompt, validator và mapper của luồng sinh quiz bằng AI. */
public final class AiQuizRules {
    /** Khoảng số câu hỏi mỗi quiz người dùng được chọn (QuizRequest.questionCount). */
    public static final int MIN_QUESTION_COUNT = 20;

    public static final int MAX_QUESTION_COUNT = 40;
    /** Tổng số lần gọi LLM tối đa (lần đầu + các lần yêu cầu sửa). */
    public static final int MAX_ATTEMPTS = 3;

    public static final int TITLE_MAX_LENGTH = 200;
    public static final int CORRECT_TEXT_MAX_LENGTH = 255;
    public static final int MIN_OPTIONS = 3;
    public static final int MAX_OPTIONS = 4;
    public static final int USER_TEXT_MAX_LENGTH = 500;
    /** Ký hiệu chỗ trống bắt buộc trong content của câu FILL_BLANK. */
    public static final String BLANK_MARKER = "___";

    private AiQuizRules() {}

    /** Reading / Listening bắt buộc có đoạn văn. So theo skill.code (cố định), không theo name (admin có thể sửa). */
    public static boolean needsPassage(String skillCode) {
        return "READING".equalsIgnoreCase(skillCode) || "LISTENING".equalsIgnoreCase(skillCode);
    }

    /** Chuẩn hoá để so trùng: bỏ khoảng trắng thừa, không phân biệt hoa thường. */
    static String normalize(String s) {
        return s.trim().replaceAll("\\s+", " ").toLowerCase();
    }

    static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    static <T> List<T> nullToEmpty(List<T> list) {
        return list == null ? List.of() : list;
    }
}
