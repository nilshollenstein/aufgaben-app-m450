package ch.bbw.aufgabenapp;

import java.time.Clock;
import java.time.LocalDate;
import java.util.NoSuchElementException;

public class TaskService {

    private final TaskRepository repository;
    private final NotificationSender sender;
    private final Clock clock;
    private final TaskValidator validator = new TaskValidator();

    public TaskService(TaskRepository repository,
            NotificationSender sender, Clock clock) {
        this.repository = repository;
        this.sender = sender;
        this.clock = clock;
    }

    public long create(Task task) {
        LocalDate today = LocalDate.now(clock);
        if (!validator.isTitleValid(task.title())
                || !validator.isPriorityValid(task.priority())
                || !validator.isDueDateValid(task.dueDate(), today)) {
            throw new IllegalArgumentException(
                    "Ungültige Aufgabe: " + task.title());
        }
        return repository.save(task);
    }

    public void complete(long id) {
        Task task = repository.findById(id).orElseThrow(() ->
                new NoSuchElementException("Aufgabe " + id + " fehlt"));
        if (task.done()) {
            throw new IllegalStateException(
                    "Aufgabe " + id + " ist bereits erledigt");
        }
        repository.update(id, new Task(task.title(), task.priority(),
                task.dueDate(), true));
    }

    public int remindDue(String recipient) {
        int sent = 0;
        for (Task task : repository.findDueOn(LocalDate.now(clock))) {
            if (!task.done()) {
                sender.send(recipient, "Erinnerung: «" + task.title()
                        + "» ist heute fällig");
                sent++;
            }
        }
        return sent;
    }
}
