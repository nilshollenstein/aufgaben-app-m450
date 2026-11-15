package ch.bbw.aufgabenapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TaskValidatorTest {

    // Festes «heute», damit die Tests an jedem Tag gleich ausgehen
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 20);

    private final TaskValidator validator = new TaskValidator();

    // ---------- Titel (TF-01 bis TF-07) ----------

    @ParameterizedTest(name = "{0}: Titel mit {1} Zeichen gültig = {2}")
    @CsvSource({
            "TF-01, 0,   false",
            "TF-02, 1,   true",
            "TF-03, 50,  true",
            "TF-04, 100, true",
            "TF-05, 101, false"
    })
    void title_length(String id, int length, boolean expected) {
        assertEquals(expected, validator.isTitleValid("a".repeat(length)));
    }

    @Test
    @DisplayName("TF-06: Titel null wird abgelehnt")
    void title_null() {
        assertFalse(validator.isTitleValid(null));
    }

    @Test
    @DisplayName("TF-07: Titel nur aus Leerzeichen wird abgelehnt")
    void title_onlySpaces() {
        assertFalse(validator.isTitleValid("   "));
    }

    @ParameterizedTest(name = "{0}: Priorität {1} gültig = {2}")
    @CsvSource({
            "TF-08, 0, false",
            "TF-09, 1, true",
            "TF-10, 3, true",
            "TF-11, 5, true",
            "TF-12, 6, false"
    })
    void priority_boundaries(String id, int priority, boolean expected) {
        assertEquals(expected, validator.isPriorityValid(priority));
    }

    @ParameterizedTest(name = "{0}: Fälligkeit heute + ({1}) Tage gültig = {2}")
    @CsvSource({
            "TF-13, -1, false",
            "TF-14,  0, true",
            "TF-15,  1, true",
            "TF-17, 30, true"
    })
    void dueDate_relativeToToday(String id, int offsetDays, boolean expected) {
        assertEquals(expected, validator.isDueDateValid(TODAY.plusDays(offsetDays), TODAY));
    }

    @Test
    @DisplayName("TF-16: Fälligkeit null (fehlend) wird abgelehnt")
    void dueDate_null() {
        assertFalse(validator.isDueDateValid(null, TODAY));
    }
}
