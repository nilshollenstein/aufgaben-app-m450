package ch.bbw.aufgabenapp;

import static ch.bbw.aufgabenapp.TaskBuilder.TODAY;
import static ch.bbw.aufgabenapp.TaskBuilder.aTask;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    private static final String RECIPIENT = "andrea@example.org";
    private static final Clock CLOCK = Clock.fixed(
            TODAY.atTime(12, 0).atZone(ZoneId.of("Europe/Zurich")).toInstant(),
            ZoneId.of("Europe/Zurich"));

    @Mock TaskRepository repository;
    @Mock NotificationSender sender;
    @Spy Clock clock = CLOCK;
    @InjectMocks TaskService service;

    // ---------- Teil 2: Stub und verify ----------

    @Test
    void create_validTask_returnsIdFromRepository() {
        Task task = aTask().build();
        when(repository.save(task)).thenReturn(42L);

        long id = service.create(task);

        assertEquals(42L, id);
        verify(repository).save(task);
    }

    @Test
    void remindDue_nothingDue_sendsNothing() {
        when(repository.findDueOn(TODAY)).thenReturn(List.of());

        assertEquals(0, service.remindDue(RECIPIENT));

        verifyNoInteractions(sender);
    }

    @Test
    void complete_unknownId_throwsAndNothingUpdated() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> service.complete(99L));

        verify(repository, never()).update(anyLong(), any());
    }

    @Test
    void complete_alreadyDone_throwsAndNothingUpdated() {
        when(repository.findById(5L)).thenReturn(Optional.of(aTask().done().build()));

        assertThrows(IllegalStateException.class, () -> service.complete(5L));

        verify(repository, never()).update(anyLong(), any());
    }

    @Test
    void remindDue_databaseDown_throwsAndSendsNothing() {
        IllegalStateException databaseDown =
                new IllegalStateException("Datenbank nicht erreichbar");
        when(repository.findDueOn(any(LocalDate.class))).thenThrow(databaseDown);

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> service.remindDue(RECIPIENT));

        assertSame(databaseDown, thrown);
        verifyNoInteractions(sender);
    }

    // ---------- Teil 3: ArgumentCaptor und Testdaten-Builder ----------

    @Test
    void complete_openTask_savesCopyMarkedAsDone() {
        when(repository.findById(7L)).thenReturn(Optional.of(
                aTask().withTitle("Altpapier").withPriority(2).build()));

        service.complete(7L);

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(repository).update(eq(7L), captor.capture());
        Task saved = captor.getValue();
        assertEquals(true, saved.done());
        assertEquals("Altpapier", saved.title());
        assertEquals(2, saved.priority());
    }

    @Test
    void remindDue_messageContainsTitle() {
        when(repository.findDueOn(TODAY)).thenReturn(List.of(
                aTask().withTitle("Steuern").dueOn(TODAY).build()));

        service.remindDue(RECIPIENT);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(sender).send(eq(RECIPIENT), captor.capture());
        assertEquals("Erinnerung: «Steuern» ist heute fällig", captor.getValue());
    }

    @Test
    void remindDue_sendsOneMessagePerOpenTask() {
        when(repository.findDueOn(TODAY)).thenReturn(List.of(
                aTask().dueOn(TODAY).build(),
                aTask().dueOn(TODAY).build(),
                aTask().dueOn(TODAY).done().build()));

        int sent = service.remindDue(RECIPIENT);

        assertEquals(2, sent);
        verify(sender, times(2)).send(eq(RECIPIENT), any());
    }

    @ParameterizedTest(name = "[{index}] Titel=''{0}'', Priorität={1}, Tage ab heute={2}")
    @CsvSource({
            "'',        3,  1",   // Titel leer
            "Einkaufen, 0,  1",   // Priorität unter der unteren Grenze
            "Einkaufen, 6,  1",   // Priorität über der oberen Grenze
            "Einkaufen, 3, -1"    // gestern fällig
    })
    void create_invalidTask_isRejectedAndNothingSaved(String title, int priority,
            int daysFromToday) {
        Task task = aTask()
                .withTitle(title)
                .withPriority(priority)
                .dueOn(TODAY.plusDays(daysFromToday))
                .build();

        assertThrows(IllegalArgumentException.class, () -> service.create(task));

        verifyNoInteractions(repository, sender);
    }
}
