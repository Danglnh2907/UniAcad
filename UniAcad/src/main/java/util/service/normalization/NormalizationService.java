package util.service.normalization;

public class NormalizationService {
    private final StringBuilder sb;
    public NormalizationService() {
        sb = new StringBuilder();
    }

    public String normalizationFullName(String input) {
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

    public String normaliizationEmail(String input)
    {
        input = input.trim().toLowerCase();
        return input;
    }

    public static void main (String[] args)
    {
        NormalizationService normalizationService = new NormalizationService();
        System.out.println(normalizationService.normalizationFullName("    ABC      bad  am"));
    }
}