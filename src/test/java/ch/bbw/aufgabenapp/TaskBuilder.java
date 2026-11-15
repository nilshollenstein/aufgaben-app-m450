package ch.bbw.aufgabenapp;

import java.time.LocalDate;

public class TaskBuilder {

    public static final LocalDate TODAY = LocalDate.of(2026, 10, 27);

    private String title = "Einkaufen";
    private int priority = 3;
    private LocalDate dueDate = TODAY.plusDays(1);
    private boolean done = false;

    public static TaskBuilder aTask() {
        return new TaskBuilder();
    }

    public TaskBuilder withTitle(String title) {
        this.title = title;
        return this;
    }

    public TaskBuilder withPriority(int priority) {
        this.priority = priority;
        return this;
    }

    public TaskBuilder dueOn(LocalDate dueDate) {
        this.dueDate = dueDate;
        return this;
    }

    public TaskBuilder done() {
        this.done = true;
        return this;
    }

    public Task build() {
        return new Task(title, priority, dueDate, done);
    }
}
