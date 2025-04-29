package util.service.normalization;

public class NormalizationService {
    private final StringBuilder sb;

    public NormalizationService() {
        sb = new StringBuilder();
    }

    /**
     * Chuẩn hóa họ tên:
     * - Xóa dư khoảng trắng.
     * - Viết hoa chữ cái đầu mỗi từ.
     */
    public String normalizationFullName(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "";
        }

        String[] words = input.trim().split("\\s+");
        sb.setLength(0); // clear StringBuilder for reuse

        for (String word : words) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    sb.append(word.substring(1).toLowerCase());
                }
                sb.append(" ");
            }
        }
        return sb.toString().trim(); // remove trailing space
    }

    /**
     * Chuẩn hóa email:
     * - Xóa dư khoảng trắng.
     * - Chuyển hết thành chữ thường.
     */
    public String normalizationEmail(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "";
        }
        return input.trim().toLowerCase();
    }

    /**
     * (Optional) Chuẩn hóa số điện thoại: xóa dấu cách, chuẩn hóa chỉ còn số + chữ số
     */
    public String normalizationPhoneNumber(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "";
        }
        return input.replaceAll("[^0-9+]", "");
    }

    public static void main(String[] args) {
        NormalizationService normalizationService = new NormalizationService();
        System.out.println(normalizationService.normalizationFullName("    ABC      bad  am")); // --> "Abc Bad Am"
        System.out.println(normalizationService.normalizationEmail("   TestEmail@Gmail.Com ")); // --> "testemail@gmail.com"
        System.out.println(normalizationService.normalizationPhoneNumber("+84 912 345 678"));    // --> "+84912345678"
    }
}
