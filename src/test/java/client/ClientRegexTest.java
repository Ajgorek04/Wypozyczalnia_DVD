package client;

import org.junit.jupiter.api.Test;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;

class ClientRegexTest {

    @Test
    void shouldParseServerFeeResponseWithDot() {
        // Taki format wysyła Twój serwer (z Locale.US)
        String serverResponse = "Opłata 5 | kwota: 10.00 zł | powód: Start | Opłacona: NIE";

        // Regex z Twojego MainClient
        Pattern p = Pattern.compile("Opłata\\s+(\\d+)\\s+\\|.*kwota:\\s*([0-9]+\\.?[0-9]*).*?\\|\\s*powód:\\s*(.*?)\\s+\\|\\s*Opłacona:\\s*(NIE|TAK)");
        Matcher m = p.matcher(serverResponse);

        assertTrue(m.find(), "Regex powinien dopasować odpowiedź serwera");
        assertEquals("5", m.group(1), "ID opłaty");
        assertEquals("10.00", m.group(2), "Kwota");
        assertEquals("Start", m.group(3), "Powód");
        assertEquals("NIE", m.group(4), "Status opłacenia");
    }

    @Test
    void shouldParseDynamicSumWithExtraText() {
        // Test trudniejszego przypadku (np. gdy doliczono czas)
        String serverResponse = "Opłata 12 | kwota: 16.50 zł | powód: Suma bieżąca (Start + 4 min) | Opłacona: NIE";

        Pattern p = Pattern.compile("Opłata\\s+(\\d+)\\s+\\|.*kwota:\\s*([0-9]+\\.?[0-9]*).*?\\|\\s*powód:\\s*(.*?)\\s+\\|\\s*Opłacona:\\s*(NIE|TAK)");
        Matcher m = p.matcher(serverResponse);

        assertTrue(m.find());
        assertEquals("12", m.group(1));
        assertEquals("16.50", m.group(2));
        assertEquals("Suma bieżąca (Start + 4 min)", m.group(3));
    }
}