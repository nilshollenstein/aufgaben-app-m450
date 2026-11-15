package ch.bbw.aufgabenapp;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TaskRepository {
    long save(Task task);
    Optional<Task> findById(long id);
    void update(long id, Task task);
    List<Task> findDueOn(LocalDate date);
}
