package backend.dev.PLA.exceptions;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Lỗi hệ thống không xác định", HttpStatus.INTERNAL_SERVER_ERROR),

    // Auth & User Business Errors
    UNAUTHENTICATED(1001, "Token không đúng hoặc đã hết hạn", HttpStatus.UNAUTHORIZED),
    ACCESS_DENIED(1002, "Bạn không có quyền thực hiện thao tác này", HttpStatus.FORBIDDEN),
    USER_EXISTED(1003, "Tên đăng nhập đã tồn tại", HttpStatus.BAD_REQUEST),
    USER_NOT_FOUND(1004, "Không tìm thấy người dùng", HttpStatus.NOT_FOUND),
    BAD_CREDENTIALS(1005, "Sai tên đăng nhập hoặc mật khẩu!", HttpStatus.UNAUTHORIZED),

    // Validation Errors - General & Username
    INVALID_INPUT(1006, "Dữ liệu đầu vào không hợp lệ", HttpStatus.BAD_REQUEST),
    USERNAME_BLANK(1007, "Tên đăng nhập không được để trống", HttpStatus.BAD_REQUEST),
    USERNAME_INVALID_SIZE(1008, "Tên đăng nhập phải từ 6 đến 50 ký tự", HttpStatus.BAD_REQUEST),
    USERNAME_INVALID_PATTERN(1009, "Tên đăng nhập chỉ được chứa chữ cái và số", HttpStatus.BAD_REQUEST),

    // Validation Errors - Password
    PASSWORD_BLANK(1010, "Mật khẩu không được để trống", HttpStatus.BAD_REQUEST),
    PASSWORD_INVALID_SIZE(1011, "Mật khẩu phải từ 6 đến 50 ký tự", HttpStatus.BAD_REQUEST),
    PASSWORD_INVALID_PATTERN(
            1012, "Mật khẩu phải có ít nhất 1 chữ hoa, 1 chữ thường, 1 số và 1 ký tự đặc biệt", HttpStatus.BAD_REQUEST),

    // Validation Errors - Email
    EMAIL_BLANK(1013, "Email không được để trống", HttpStatus.BAD_REQUEST),
    EMAIL_INVALID_FORMAT(1014, "Định dạng email không hợp lệ", HttpStatus.BAD_REQUEST),

    // Validation Errors - Phone Number
    PHONE_BLANK(1015, "Số điện thoại không được để trống", HttpStatus.BAD_REQUEST),
    PHONE_INVALID_SIZE(1016, "Số điện thoại phải từ 10 đến 15 ký tự", HttpStatus.BAD_REQUEST),
    PHONE_INVALID_PATTERN(1017, "Số điện thoại chỉ được chứa chữ số", HttpStatus.BAD_REQUEST),

    // Duplicate Errors
    EMAIL_EXISTED(1018, "Email này đã được sử dụng", HttpStatus.BAD_REQUEST),
    PHONE_EXISTED(1019, "Số điện thoại này đã được sử dụng", HttpStatus.BAD_REQUEST),

    // Validation Errors - Score (0.0 - 9.0)
    CURRENT_SCORE_MIN(1020, "Điểm hiện tại tối thiểu phải là 0.0", HttpStatus.BAD_REQUEST),
    CURRENT_SCORE_MAX(1021, "Điểm hiện tại tối đa không được vượt quá 9.0", HttpStatus.BAD_REQUEST),
    TARGET_SCORE_MIN(1022, "Điểm mục tiêu tối thiểu phải là 0.0", HttpStatus.BAD_REQUEST),
    TARGET_SCORE_MAX(1023, "Điểm mục tiêu tối đa không được vượt quá 9.0", HttpStatus.BAD_REQUEST),

    // Validation Errors - flashcard
    FRONT_CONTENT_BLANK(1024, "nội dung ở mặt trước không được để trống", HttpStatus.BAD_REQUEST),
    BACK_CONTENT_BLANK(1025, "nội dung phía sau không được bỏ trống", HttpStatus.BAD_REQUEST),
    RATING_MIN(1026, "rating tối thiểu là 1", HttpStatus.BAD_REQUEST),
    RATING_MAX(1027, "rating tối đa là 4", HttpStatus.BAD_REQUEST),
    FLASHCARD_DETAIL_EXISTED(1028, "Flashcard này đã có trong deck rồi!", HttpStatus.BAD_REQUEST),
    FLASHCARD_ID_REQUIRED(1029, "id flashcard không được bỏ trống", HttpStatus.BAD_REQUEST),
    RATING_REQUIRED(1030, "rating không được để trống", HttpStatus.BAD_REQUEST),
    FLASHCARD_NOT_FOUND(1031, "Không tìm thấy flashcard!", HttpStatus.NOT_FOUND),
    // validation Errors - quiz
    TOPIC_BLANK(1032, "Topic không được để trống ", HttpStatus.BAD_REQUEST),
    SKILL_BLANK(1033, "Skill không được để trống ", HttpStatus.BAD_REQUEST),
    QUIZ_TITLE_SIZE(1034, "title tối đa 200 kí tự", HttpStatus.BAD_REQUEST),
    // Validation Errors - question
    QUESTION_CONTENT_BLANK(1035, "nội dung của content không được để trống", HttpStatus.BAD_REQUEST),
    QUESTION_CONTENT_PATTERN(1036, "content câu hỏi chỉ gồm chữ cái , số và dấu cách", HttpStatus.BAD_REQUEST),
    // Validate Errors - option
    QUESTION_ID_BLANK(1037, "id của câu hỏi không được null", HttpStatus.BAD_REQUEST),
    OPTION_CONTENT_BLANK(1038, "nội dung của câu trả lời không được trống ", HttpStatus.BAD_REQUEST),
    OPTION_CONTENT_PATTERN(1039, "content câu trả lời chỉ gồm chữ cái , số và dấu cách", HttpStatus.BAD_REQUEST),
    ANSWER_BLANK(1040, "trạng thái lựa chọn không được bỏ trống", HttpStatus.BAD_REQUEST),
    // Validate Errors - deck
    DECK_ID_REQUIRED(1041, "id deck không được bỏ trống", HttpStatus.BAD_REQUEST),
    DECK_NAME_BLANK(1042, "tên deck không được để trống", HttpStatus.BAD_REQUEST),
    DECK_NAME_SIZE(1043, "tên deck tối đa 100 kí tự", HttpStatus.BAD_REQUEST),
    DECK_DESCRIPTION_SIZE(1044, "mô tả deck tối đa 255 kí tự", HttpStatus.BAD_REQUEST),
    DECK_EXISTED(1045, "Bạn đã có deck với tên này rồi!", HttpStatus.BAD_REQUEST),
    DECK_NOT_FOUND(1046, "Không tìm thấy deck!", HttpStatus.NOT_FOUND),
    // Validate Errors - quiz, question, option theo schema mới
    QUIZ_TITLE_BLANK(1047, "title của quiz không được để trống", HttpStatus.BAD_REQUEST),
    CUSTOM_TOPIC_SIZE(1048, "chủ đề tự nhập tối đa 200 kí tự", HttpStatus.BAD_REQUEST),
    QUIZ_DIFFICULTY_RANGE(1049, "độ khó của quiz phải từ 1 đến 3", HttpStatus.BAD_REQUEST),
    AUDIO_URL_SIZE(1050, "audio url tối đa 500 kí tự", HttpStatus.BAD_REQUEST),
    QUIZ_ID_BLANK(1051, "id của quiz không được null", HttpStatus.BAD_REQUEST),
    POSITION_BLANK(1052, "vị trí không được để trống", HttpStatus.BAD_REQUEST),
    CORRECT_TEXT_SIZE(1053, "đáp án tối đa 255 kí tự", HttpStatus.BAD_REQUEST),

    // Topic exception
    TOPIC_GROUP_EXISTED(1054, "Topic group với tên và loại này đã tồn tại", HttpStatus.BAD_REQUEST),
    TOPIC_GROUP_NOT_FOUND(1055, "Không tìm thấy topic group!", HttpStatus.NOT_FOUND),
    TOPIC_NOT_FOUND(1056, "Không tìm thấy topic!", HttpStatus.NOT_FOUND),
    TOPIC_SLUG_EXISTED(1057, "slug của topic đã tồn tại", HttpStatus.BAD_REQUEST),
    TOPIC_IN_USE(1058, "Topic đang được sử dụng bởi quiz, không thể xóa", HttpStatus.BAD_REQUEST),
    TOPIC_GROUP_IN_USE(1059, "Topic group vẫn còn topic, không thể xóa", HttpStatus.BAD_REQUEST),

    // Request errors (lỗi chuẩn Spring MVC)
    MALFORMED_REQUEST_BODY(
            1060,
            "Body không hợp lệ: JSON sai định dạng, thiếu body hoặc giá trị không đúng kiểu",
            HttpStatus.BAD_REQUEST),
    METHOD_NOT_ALLOWED(1061, "Phương thức HTTP không được hỗ trợ cho endpoint này", HttpStatus.METHOD_NOT_ALLOWED),
    UNSUPPORTED_MEDIA_TYPE(1062, "Content-Type không được hỗ trợ", HttpStatus.UNSUPPORTED_MEDIA_TYPE),
    ENDPOINT_NOT_FOUND(1063, "Không tìm thấy endpoint", HttpStatus.NOT_FOUND),

    // Skill exception
    SKILL_NOT_FOUND(1064, "Không tìm thấy skill!", HttpStatus.NOT_FOUND),
    SKILL_CODE_EXISTED(1065, "code của skill đã tồn tại", HttpStatus.BAD_REQUEST),
    SKILL_IN_USE(1066, "Skill đang được sử dụng, không thể xóa", HttpStatus.BAD_REQUEST),

    // Quiz exception
    QUIZ_NOT_FOUND(1067, "Không tìm thấy quiz!", HttpStatus.NOT_FOUND),
    QUIZ_HAS_ATTEMPTS(1068, "Quiz đã có lượt làm bài, không thể xoá", HttpStatus.CONFLICT),
    QUIZ_GENERATING(1069, "Quiz đang được sinh, không thể sửa hoặc xoá", HttpStatus.CONFLICT),
    QUIZ_TOPIC_EXACTLY_ONE(1070, "Phải có đúng một trong topicId hoặc customTopic", HttpStatus.BAD_REQUEST),
    QUIZ_SKILL_INVALID(1071, "Skill không tồn tại", HttpStatus.BAD_REQUEST),
    QUIZ_SKILL_TOPIC_MISMATCH(1072, "Topic không thuộc skill đã chọn", HttpStatus.BAD_REQUEST),
    QUIZ_FORMAT_INVALID(1073, "Dạng bài không tồn tại hoặc không thuộc skill đã chọn", HttpStatus.BAD_REQUEST),
    QUIZ_DIFFICULTY_BLANK(1074, "độ khó của quiz không được để trống", HttpStatus.BAD_REQUEST),
    QUIZ_DIFFICULTY_OUT_OF_RANGE(1075, "độ khó của quiz phải từ 1 đến 5", HttpStatus.BAD_REQUEST),
    QUIZ_SORT_INVALID(
            1076,
            "sort không hợp lệ, dạng field,asc|desc với field là createdAt, title, difficulty",
            HttpStatus.BAD_REQUEST),

    // Pagination
    PAGE_INVALID(1077, "page phải >= 0", HttpStatus.BAD_REQUEST),
    PAGE_SIZE_INVALID(1078, "size phải từ 1 đến 100", HttpStatus.BAD_REQUEST),

    // Passage exception
    PASSAGE_NOT_FOUND(1079, "Không tìm thấy passage!", HttpStatus.NOT_FOUND),
    PASSAGE_HAS_QUESTIONS(1080, "Passage đang có câu hỏi gắn vào, phải xoá hoặc gỡ câu hỏi trước", HttpStatus.CONFLICT),
    PASSAGE_POSITION_DUPLICATED(1081, "position đã được dùng bởi passage khác trong quiz", HttpStatus.BAD_REQUEST),
    PASSAGE_POSITION_INVALID(1082, "position của passage phải từ 1 đến 32767", HttpStatus.BAD_REQUEST),
    PASSAGE_CONTENT_REQUIRED(1083, "Passage phải có content hoặc audioUrl", HttpStatus.BAD_REQUEST),
    PASSAGE_AUDIO_URL_INVALID(1084, "audioUrl phải là URL http/https, tối đa 500 ký tự", HttpStatus.BAD_REQUEST),

    // Question exception
    QUESTION_NOT_FOUND(1085, "Không tìm thấy câu hỏi!", HttpStatus.NOT_FOUND),
    QUESTION_TYPE_BLANK(1086, "loại câu hỏi không được để trống", HttpStatus.BAD_REQUEST),
    QUESTION_POSITION_INVALID(1087, "position của câu hỏi phải từ 1 đến 32767", HttpStatus.BAD_REQUEST),
    QUESTION_POSITION_DUPLICATED(1088, "position đã được dùng bởi câu hỏi khác trong quiz", HttpStatus.BAD_REQUEST),
    QUESTION_CORRECT_TEXT_INVALID(
            1089, "FILL_BLANK phải có correctText, SINGLE_CHOICE không được có correctText", HttpStatus.BAD_REQUEST),
    PASSAGE_NOT_IN_QUIZ(1090, "Passage không thuộc quiz này", HttpStatus.BAD_REQUEST),
    QUESTION_GRAMMAR_TOPIC_INVALID(1091, "grammarTopicId phải là topic thuộc nhóm GRAMMAR", HttpStatus.BAD_REQUEST),
    QUIZ_QUESTION_LIMIT(1092, "Một quiz tối đa 100 câu hỏi", HttpStatus.BAD_REQUEST),
    QUESTION_HAS_OPTIONS(
            1093, "Câu hỏi đang có đáp án, phải xoá đáp án trước khi đổi sang FILL_BLANK", HttpStatus.CONFLICT),

    // Option exception
    OPTION_NOT_FOUND(1095, "Không tìm thấy đáp án!", HttpStatus.NOT_FOUND),
    OPTION_NOT_ALLOWED(1096, "Câu FILL_BLANK không có đáp án lựa chọn", HttpStatus.BAD_REQUEST),
    OPTION_POSITION_INVALID(1097, "position của đáp án phải từ 1 đến 32767", HttpStatus.BAD_REQUEST),
    OPTION_POSITION_DUPLICATED(1098, "position đã được dùng bởi đáp án khác của câu", HttpStatus.BAD_REQUEST),
    QUESTION_OPTION_LIMIT(1099, "Một câu hỏi tối đa 6 đáp án", HttpStatus.BAD_REQUEST),
    ;

    private final int code;
    private final String message;
    private final HttpStatusCode statusCode;
}
